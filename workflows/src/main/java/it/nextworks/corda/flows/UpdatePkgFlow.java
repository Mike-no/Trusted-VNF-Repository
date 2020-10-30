package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgOfferContract;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static it.nextworks.corda.flows.UpdatePkgFlowUtils.*;

public class UpdatePkgFlow {

    /**
     * This exception will be thrown if the UniqueIdentifier specified
     * does not correspond to a package stored in the vault.
     */
    public static class NonExistentPkgException extends FlowException {
        public NonExistentPkgException(UniqueIdentifier pkgId) {
            super(nonExistentPkg + pkgId);
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class DevInitiation extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier linearId;

        private final String name;
        private final String description;
        private final String version;
        private final String imageLink;
        private final ProductOfferingPrice poPrice;

        private final Step RETRIEVING_PKG_FROM_LID = new Step(UpdatePkgFlowUtils.RETRIEVING_PKG_FROM_LID);
        private final Step GENERATING_TRANSACTION  = new Step(UpdatePkgFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION   = new Step(UpdatePkgFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION     = new Step(UpdatePkgFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS         = new Step(UpdatePkgFlowUtils.GATHERING_SIGNS) {
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION  = new Step(UpdatePkgFlowUtils.FINALISING_TRANSACTION) {
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
                RETRIEVING_PKG_FROM_LID,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the Initiating flow class, the following parameters will be used to build the transaction
         * @param linearId    Linear ID of the package that we want update
         * @param name        updated name of the package to build in the transaction
         * @param description updated description of the package to build in the transaction
         * @param version     updated version of the package to build in the transaction
         * @param imageLink   updated customized marketplace cover art location of the package to build in the transaction
         * @param poPrice     updated product offering price of the package to build in the transaction
         */
        public DevInitiation(UniqueIdentifier linearId,
                             String name,
                             String description,
                             String version,
                             String imageLink,
                             ProductOfferingPrice poPrice) {
            if(linearId == null)
                throw new IllegalArgumentException(nullLinearId);

            this.linearId    = linearId;

            this.name        = name;
            this.description = description;
            this.version     = version;
            this.imageLink   = imageLink;
            this.poPrice     = poPrice;
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

            /* Set the current step to RETRIEVING_PKG_FROM_LID and proceed to query the vault */
            progressTracker.setCurrentStep(RETRIEVING_PKG_FROM_LID);

            /* Query for unconsumed linear state for given linear ID */
            QueryCriteria.LinearStateQueryCriteria queryCriteria =
                    new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId.getId()),
                            null, Vault.StateStatus.UNCONSUMED);
            final List<StateAndRef<PkgOfferState>> lst = getServiceHub().getVaultService()
                    .queryBy(PkgOfferState.class, queryCriteria).getStates();
            if(lst.size() == 0)
                throw new NonExistentPkgException(linearId);

            final PkgOfferState oldPkgOfferState = lst.get(0).getState().getData();

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final PkgOfferState newPkgOfferState = new PkgOfferState(oldPkgOfferState.getLinearId(), name, description,
                    version, oldPkgOfferState.getPkgInfoId(), imageLink, oldPkgOfferState.getPkgType(), poPrice, author, repositoryNode);
            final Command<PkgOfferContract.Commands.UpdatePkg> txCommand = new Command<>(
                    new PkgOfferContract.Commands.UpdatePkg(), ImmutableList.of(author.getOwningKey(),
                    repositoryNode.getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addInputState(lst.get(0))
                    .addOutputState(newPkgOfferState, PkgOfferContract.ID)
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
                    ImmutableList.of(repositoryNodeSession)));

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new FinalityFlow(fullySignedTx, ImmutableList.of(repositoryNodeSession)));
        }
    }

    @InitiatedBy(DevInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession devSession;

        /**
         * Constructor of the flow initiated by the DevInitiation class
         * @param devSession session with the developer that want to update his package
         */
        public RepositoryNodeAcceptor(FlowSession devSession) { this.devSession = devSession; }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession devSession, ProgressTracker progressTracker) {
                    super(devSession, progressTracker);
                }

                /**
                 * Override the checkTransaction function to define the behaviour of the
                 * repositoryNode when accepts new package update
                 */
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) throws NonExistentPkgException {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    if(!(output instanceof PkgOfferState))
                        throw new IllegalArgumentException(notPkgStateErr);
                    PkgOfferState newPkgOfferState = (PkgOfferState)output;

                    /* Query for unconsumed linear state for the linear ID specified in the updated state */
                    UniqueIdentifier linearId = newPkgOfferState.getLinearId();
                    QueryCriteria.LinearStateQueryCriteria queryCriteria =
                            new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId.getId()),
                                    null, Vault.StateStatus.UNCONSUMED);
                    final List<StateAndRef<PkgOfferState>> lst = getServiceHub().getVaultService()
                            .queryBy(PkgOfferState.class, queryCriteria).getStates();
                    if(lst.size() == 0)
                        throw new NonExistentPkgException(linearId);
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
