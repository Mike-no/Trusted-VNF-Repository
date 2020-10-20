package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.FeeAgreementContract;
import it.nextworks.corda.states.FeeAgreementState;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static it.nextworks.corda.flows.EstablishFeeAgreementFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class EstablishFeeAgreementFlow {
    /**
     * This exception will be thrown if the developer has already
     * establish a fee agreement with the Repository Node.
     */
    public static class AlreadyEstablishedAgreementException extends FlowException {
        public AlreadyEstablishedAgreementException() {
            super(AlreadyEstablishedFee);
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class DevInitiation extends FlowLogic<SignedTransaction> {

        private final int maxAcceptableFee;

        /**
         * Constructor of the Initiating flow class.
         * @param maxAcceptableFee max % fee accepted by the developer
         */
        public DevInitiation(int maxAcceptableFee) { this.maxAcceptableFee = maxAcceptableFee; }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {
                private SignTxFlow(FlowSession repositoryNodeSession, ProgressTracker progressTracker) {
                    super(repositoryNodeSession, progressTracker);
                }

                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx) {
                    requireThat(require -> {
                        ContractState output = stx.getTx().getOutputs().get(0).getData();
                        require.using(notFeeAgreementErr, output instanceof FeeAgreementState);
                        FeeAgreementState feeAgreementState = (FeeAgreementState)output;

                        require.using(tooHighFee, feeAgreementState.getFee() <= maxAcceptableFee);

                        return null;
                    });
                }
            }

            /*
             * Retrieving the Repository Node identity that will be used as <repositoryNode>
             * parameters in the transaction.
             */
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);

            /* Check and Sign the transaction, get the hash value of the obtained transaction */
            final SignTxFlow signTxFlow =
                    new SignTxFlow(repositoryNodeSession, SignTransactionFlow.Companion.tracker());
            final SecureHash txId = subFlow(signTxFlow).getId();

            /*
             * Receive the transaction that will be stored in the vault and compare it's hash value
             * with the previously saved hash; if it's the same, proceed storing the transaction.
             */
            return subFlow(new ReceiveFinalityFlow(repositoryNodeSession, txId));
        }
    }

    @InitiatedBy(DevInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession devSession;

        private final Step VERIFY_AGREEMENT_EXISTENCE =
                new Step(EstablishFeeAgreementFlowUtils.VERIFY_AGREEMENT_EXISTENCE);
        private final Step GENERATING_TRANSACTION     = new Step(EstablishFeeAgreementFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION      = new Step(EstablishFeeAgreementFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION        = new Step(EstablishFeeAgreementFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS            = new Step(EstablishFeeAgreementFlowUtils.GATHERING_SIGNS) {
            @Override
            public ProgressTracker childProgressTracker() { return CollectSignaturesFlow.Companion.tracker(); }
        };
        private final Step FINALISING_TRANSACTION     = new Step(EstablishFeeAgreementFlowUtils.FINALISING_TRANSACTION) {
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
                VERIFY_AGREEMENT_EXISTENCE,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the flow initiated by the DevInitiation class
         * @param devSession session with the developer that want to establish a fee
         */
        public RepositoryNodeAcceptor(FlowSession devSession) { this.devSession = devSession; }

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

            final Party repositoryNode = getOurIdentity();
            final Party devNode = devSession.getCounterparty();

            /* Set the current step to VERIFY_AGREEMENT_EXISTENCE and proceed to query the vault */
            progressTracker.setCurrentStep(VERIFY_AGREEMENT_EXISTENCE);

            QueryCriteria.VaultQueryCriteria queryCriteria =
                    new QueryCriteria.VaultQueryCriteria()
                            .withExactParticipants(ImmutableList.of(devNode, repositoryNode));
            List<StateAndRef<FeeAgreementState>> lst =
                    getServiceHub().getVaultService().queryBy(FeeAgreementState.class, queryCriteria).getStates();
            if(lst.size() == 1)
                throw new AlreadyEstablishedAgreementException();

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final FeeAgreementState feeAgreementState =
                    new FeeAgreementState(devNode, repositoryNode);
            final Command<FeeAgreementContract.Commands.EstablishFeeAgreement> txCommand =
                    new Command<>(new FeeAgreementContract.Commands.EstablishFeeAgreement(),
                            ImmutableList.of(devNode.getOwningKey(),
                                    repositoryNode.getOwningKey()));
            final TransactionBuilder txBuilder = new TransactionBuilder(notary)
                    .addOutputState(feeAgreementState, FeeAgreementContract.ID)
                    .addCommand(txCommand);

            /* Set the current step to VERIFYING_TRANSACTION and proceed to call the verify function */
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

            txBuilder.verify(getServiceHub());

            /* Set the current step to SIGNING_TRANSACTION and proceed to sign the latter */
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);

            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder);

            /* Set the current step to GATHERING_SIGNS and starts a gathering sub-flow */
            progressTracker.setCurrentStep(GATHERING_SIGNS);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableList.of(devSession)));

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new FinalityFlow(fullySignedTx, ImmutableList.of(devSession)));
        }
    }
}
