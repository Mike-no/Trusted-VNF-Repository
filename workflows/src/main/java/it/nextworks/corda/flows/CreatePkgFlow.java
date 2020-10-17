package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.nextworks.corda.contracts.PkgOfferContract;
import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import org.jetbrains.annotations.NotNull;

import java.util.Currency;

import static it.nextworks.corda.flows.CreatePkgFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CreatePkgFlow {
    @InitiatingFlow
    @StartableByRPC
    public static class DevInitiation extends FlowLogic<SignedTransaction> {

        private final String name;
        private final String description;
        private final String version;
        private final String pkgInfoId;
        private final String imageLink;
        private final Amount<Currency> price;
        private final PkgOfferState.PkgType pkgType;

        private final Step GENERATING_TRANSACTION = new Step(CreatePkgFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION  = new Step(CreatePkgFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION    = new Step(CreatePkgFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS        = new Step(CreatePkgFlowUtils.GATHERING_SIGNS){
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step(CreatePkgFlowUtils.FINALISING_TRANSACTION) {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
         * function.
         */
        private final ProgressTracker progressTracker = new ProgressTracker(
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the Initiating flow class, the following parameters will be used to build the transaction
         * @param name           name of the package to build in the transaction
         * @param description    description of the package to build in the transaction
         * @param version        version of the package to build in the transaction
         * @param pkgInfoId      pkg info id of the package to build in the transaction
         * @param imageLink      customized marketplace cover art location of the package to build in the transaction
         * @param price          price of the package to build in the transaction
         * @param pkgType        type of the package (VNF or PNF) to build in the transaction
         */
        public DevInitiation(String name,
                             String description,
                             String version,
                             String pkgInfoId,
                             String imageLink,
                             Amount<Currency> price,
                             PkgOfferState.PkgType pkgType) {
            this.name           = name;
            this.description    = description;
            this.version        = version;
            this.pkgInfoId      = pkgInfoId;
            this.imageLink      = imageLink;
            this.price          = price;
            this.pkgType        = pkgType;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            /* Retrieving a reference to notary using the serviceHub [Production Method] */
            final Party notary = getServiceHub()
                    .getNetworkMapCache()
                    .getNotary(CordaX500Name.parse(notaryX500Name));
            /*
             * Retrieving our identity and the Repository Node identity that will be used as <author>
             * and <repositoryNode> parameters in the transaction
             */
            final Party author = getOurIdentity();
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final PkgOfferState pkgOfferState = new PkgOfferState(new UniqueIdentifier(), name, description,
                    version, pkgInfoId, imageLink, price, pkgType, author, repositoryNode);
            final Command<PkgOfferContract.Commands.RegisterPkg> txCommand = new Command<>(
                    new PkgOfferContract.Commands.RegisterPkg(), ImmutableList.of(author.getOwningKey(),
                    repositoryNode.getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(pkgOfferState, PkgOfferContract.ID)
                    .addCommand(txCommand);

            /* Set the current step to VERIFYING_TRANSACTION and proceed to call the verify function */
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

            txBuilder.verify(getServiceHub());

            /* Set the current step to SIGNING_TRANSACTION and proceed to sign the latter */
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            /* Set the current step to GATHERING_SIGNS and starts a gathering sub-flow */
            progressTracker.setCurrentStep(GATHERING_SIGNS);

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);
            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableSet.of(repositoryNodeSession)));

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(repositoryNodeSession)));
        }
    }

    @InitiatedBy(DevInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession devSession;

        /**
         * Constructor of the flow initiated by the DevInitiation class
         * @param devSession session with the developer that want to submit his package
         */
        public RepositoryNodeAcceptor(FlowSession devSession) {
            this.devSession = devSession;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession devSession, ProgressTracker progressTracker) {
                    super(devSession, progressTracker);
                }

                /**
                 * Override the checkTransaction function to define the behaviour of the
                 * repositoryNode when accepts new package
                 */
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using(notPkgStateErr, output instanceof PkgOfferState);

                        // TODO control that the developer has established a fee

                        return null;
                    });
                }
            }
            /* Check and Sign the transaction, get the hash value of the obtained transaction */
            final SignTxFlow signTxFlow = new SignTxFlow(devSession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            /*
             * Receive the transaction that will be stored in the vault and compare it's hash value
             * with the previously saved hash; if it's the same, proceed storing the transaction
             */
            return subFlow(new ReceiveFinalityFlow(devSession, txId));
        }
    }
}
