package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.nextworks.corda.contracts.VnfLicenseContract;
import it.nextworks.corda.states.VnfLicenseState;
import it.nextworks.corda.states.VnfState;
import kotlin.Pair;
import net.corda.core.contracts.*;
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
import net.corda.finance.workflows.asset.CashUtils;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.flows.BuyVnfFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.contracts.utils.StateSumming.sumCashBy;
import static net.corda.finance.workflows.GetBalances.getCashBalance;

public class BuyVnfFlow {

    /**
     * This exception will be thrown if the UniqueIdentifier specified
     * does not correspond to a VNF stored in the repository node.
     */
    public static class NonExistentVnfException extends FlowException {
        public NonExistentVnfException(UniqueIdentifier vnfId) {
            super(nonExistentVnf + vnfId);
        }
    }

    /**
     * As the transaction is contractually valid this exception should never be thrown
     * and it is defined just as reminder.
     */
    public static class UnexpectedInvalidPriceException extends FlowException {
        public UnexpectedInvalidPriceException() {
            super(unexpectedInvalidPrice);
        }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class VnfBuyerInitiation extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier vnfId;
        private final Amount<Currency> price;

        private final Step SENDING_VNF_ID         = new Step(BuyVnfFlowUtils.SENDING_VNF_ID);
        private final Step RECEIVING_VNF_INFO     = new Step(BuyVnfFlowUtils.RECEIVING_VNF_INFO);
        private final Step VERIFYING_VNF_INFO     = new Step(BuyVnfFlowUtils.VERIFYING_VNF_INFO);
        private final Step GENERATING_TRANSACTION = new Step(BuyVnfFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION  = new Step(BuyVnfFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION    = new Step(BuyVnfFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS        = new Step(BuyVnfFlowUtils.GATHERING_SIGNS){
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step(BuyVnfFlowUtils.FINALISING_TRANSACTION) {
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
                SENDING_VNF_ID,
                RECEIVING_VNF_INFO,
                VERIFYING_VNF_INFO,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the Initiating flow class,
         * the following parameters will be used to build the transaction
         * @param vnfId ID of the VNF to buy
         */
        public VnfBuyerInitiation(UniqueIdentifier vnfId, Amount<Currency> price) {
            this.vnfId = vnfId;
            this.price = price;
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
             * Retrieving our identity and the Repository Node identity that will be used as <buyer>
             * and <repositoryNode> parameters in the transaction
             */
            final Party buyer = getOurIdentity();
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            /* Set the current step to SENDING_VNF_ID and proceed to send the VNF ID */
            progressTracker.setCurrentStep(SENDING_VNF_ID);

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);
            repositoryNodeSession.send(vnfId);

            /* Set the current step to RECEIVING_VNF_INFO and proceed to retrieve the VNF info */
            progressTracker.setCurrentStep(RECEIVING_VNF_INFO);

            final StateAndRef<VnfState> vnfStateAndRef = receiveAndValidateVnfState(repositoryNodeSession,
                    repositoryNode, price);
            final VnfState vnfState = vnfStateAndRef.getState().getData();

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final VnfLicenseState vnfLicenseState = new VnfLicenseState(vnfStateAndRef, vnfState.getRepositoryLink(),
                    vnfState.getRepositoryHash(), buyer, repositoryNode);
            final Command<VnfLicenseContract.Commands.BuyVNF> txCommand = new Command<>(
                    new VnfLicenseContract.Commands.BuyVNF(), ImmutableList.of(buyer.getOwningKey(),
                    repositoryNode.getOwningKey()));
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), (Currency)price.getToken());
            if(cashBalance.getQuantity() < price.getQuantity())
                throw new IllegalArgumentException(missingCash);

            Pair<TransactionBuilder, List<PublicKey>> txKeysPair =
                    CashUtils.generateSpend(getServiceHub(), txBuilder, price, getOurIdentityAndCert(), repositoryNode);
            final TransactionBuilder tx = txKeysPair.getFirst();
            tx.addOutputState(vnfLicenseState, VnfLicenseContract.ID).addCommand(txCommand);

            /* Set the current step to VERIFYING_TRANSACTION and proceed to call the verify function */
            progressTracker.setCurrentStep(VERIFYING_TRANSACTION);

            tx.verify(getServiceHub());

            /* Set the current step to SIGNING_TRANSACTION and proceed to sign the latter */
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);

            List<PublicKey> keysToSign = txKeysPair.getSecond();
            keysToSign.add(buyer.getOwningKey());
            final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(tx, keysToSign);

            /* Set the current step to GATHERING_SIGNS and starts a gathering sub-flow */
            progressTracker.setCurrentStep(GATHERING_SIGNS);

            final SignedTransaction fullySignedTx = subFlow(new CollectSignaturesFlow(partSignedTx,
                    ImmutableSet.of(repositoryNodeSession)));

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new FinalityFlow(fullySignedTx, ImmutableSet.of(repositoryNodeSession)));
        }

        @Suspendable
        private StateAndRef<VnfState> receiveAndValidateVnfState(FlowSession repositoryNodeSession,
                                                                 Party repositoryNode,
                                                                 Amount<Currency> price) throws FlowException {
            /* Retrieve the VNF info and verify that a single VnfState has been received */
            final List<StateAndRef<VnfState>> receivedObjects =
                    subFlow(new ReceiveStateAndRefFlow<>(repositoryNodeSession));

            /* Set the current step to VERIFYING_VNF_INFO and proceed to verify the received VnfState */
            progressTracker.setCurrentStep(VERIFYING_VNF_INFO);

            return requireThat(require -> {
                require.using(receivedTooMuchStates, receivedObjects.size() == 1);
                final StateAndRef<VnfState> vnfStateAndRef = receivedObjects.get(0);
                final VnfState vnfState = vnfStateAndRef.getState().getData();
                require.using(requestedVnfErr, vnfId.equals(vnfState.getLinearId()));
                require.using(priceMismatch, price.equals(vnfState.getPrice()));
                require.using(repositoryNodeMismatch, repositoryNode.equals(vnfState.getRepositoryNode()));
                return vnfStateAndRef;
            });
        }
    }

    @InitiatedBy(BuyVnfFlow.VnfBuyerInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession buyerSession;

        private final Step AWAITING_VNF_ID        = new Step(BuyVnfFlowUtils.AWAITING_VNF_ID);
        private final Step VERIFYING_RCV_DATA     = new Step(BuyVnfFlowUtils.VERIFYING_RCV_DATA);
        private final Step SENDING_VNF_INFO       = new Step(BuyVnfFlowUtils.SENDING_VNF_INFO);
        private final Step FINALISING_TRANSACTION = new Step(BuyVnfFlowUtils.FINALISING_TRANSACTION) {
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
                AWAITING_VNF_ID,
                VERIFYING_RCV_DATA,
                SENDING_VNF_INFO,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the flow initiated by the VnfBuyerInitiation class
         * @param buyerSession session with the buyer that want to purchase a VNF
         */
        public RepositoryNodeAcceptor(FlowSession buyerSession) {
            this.buyerSession = buyerSession;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {

                private final Amount<Currency> price;

                private SignTxFlow(FlowSession buyerSession, ProgressTracker progressTracker, Amount<Currency> price) {
                    super(buyerSession, progressTracker);
                    this.price = price;
                }

                /**
                 * Override the checkTransaction function to define the behaviour of the
                 * repositoryNode when emit a license after a purchase.
                 * N.B here the transaction is contractually valid
                 */
                @Override
                protected void checkTransaction(SignedTransaction stx) throws UnexpectedInvalidPriceException {
                    if(!withoutIssuer(sumCashBy(stx.getTx().getOutputStates(), getOurIdentity())).equals(price))
                        throw new UnexpectedInvalidPriceException();
                }
            }

            /* Set the current step to AWAITING_VNF_ID and proceed to call */
            progressTracker.setCurrentStep(AWAITING_VNF_ID);

            final StateAndRef<VnfState> vnfStateAndRef = buyerSession.receive(UniqueIdentifier.class).unwrap(data -> {
                /* Set the current step to VERIFYING_RCV_DATA and proceed to verify the received data */
                progressTracker.setCurrentStep(VERIFYING_RCV_DATA);

                /* Query for unconsumed linear state for given linear ID */
                QueryCriteria.LinearStateQueryCriteria queryCriteria =
                        new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(data.getId()),
                        null, Vault.StateStatus.UNCONSUMED);
                final List<StateAndRef<VnfState>> lst = getServiceHub().getVaultService()
                        .queryBy(VnfState.class, queryCriteria).getStates();
                if(lst.size() == 0)
                    throw new NonExistentVnfException(data);

                return lst.get(0);
            });

            /* Set the current step to SENDING_VNF_INFO and proceed to send the requested VNF info */
            progressTracker.setCurrentStep(SENDING_VNF_INFO);

            subFlow(new SendStateAndRefFlow(buyerSession, ImmutableList.of(vnfStateAndRef)));

            /* Check and Sign the transaction, get the hash value of the obtained transaction */
            final SignTxFlow signTxFlow = new SignTxFlow(buyerSession, SignTransactionFlow.Companion.tracker(),
                    vnfStateAndRef.getState().getData().getPrice());
            final SecureHash txId = subFlow(signTxFlow).getId();

            /*
             * Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow
             * Receive the transaction that will be stored in the vault and compare it's hash value
             * with the previously saved hash; if it's the same, proceed storing the transaction
             */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new ReceiveFinalityFlow(buyerSession, txId));
        }
    }
}
