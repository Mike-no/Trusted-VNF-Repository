package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgLicenseContract;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.PkgLicenseState;
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

import static it.nextworks.corda.flows.BuyPkgFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.contracts.utils.StateSumming.sumCashBy;
import static net.corda.finance.workflows.GetBalances.getCashBalance;

public class BuyPkgFlow {

    /**
     * This exception will be thrown if the UniqueIdentifier specified
     * does not correspond to a package stored in the repository node.
     */
    public static class NonExistentPkgException extends FlowException {
        public NonExistentPkgException(UniqueIdentifier pkgId) {
            super(nonExistentPkg + pkgId);
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
    public static class PkgBuyerInitiation extends FlowLogic<SignedTransaction> {

        private final UniqueIdentifier pkgId;
        private final Amount<Currency> price;

        private final Step SENDING_PKG_ID         = new Step(BuyPkgFlowUtils.SENDING_PKG_ID);
        private final Step RECEIVING_PKG_INFO     = new Step(BuyPkgFlowUtils.RECEIVING_PKG_INFO);
        private final Step VERIFYING_PKG_INFO     = new Step(BuyPkgFlowUtils.VERIFYING_PKG_INFO);
        private final Step GENERATING_TRANSACTION = new Step(BuyPkgFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION  = new Step(BuyPkgFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION    = new Step(BuyPkgFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS        = new Step(BuyPkgFlowUtils.GATHERING_SIGNS){
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step(BuyPkgFlowUtils.FINALISING_TRANSACTION) {
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
                SENDING_PKG_ID,
                RECEIVING_PKG_INFO,
                VERIFYING_PKG_INFO,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGNS,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the Initiating flow class,
         * the following parameters will be used to build the transaction
         * @param pkgId ID of the package to buy
         */
        public PkgBuyerInitiation(UniqueIdentifier pkgId, Amount<Currency> price) {
            this.pkgId = pkgId;
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

            /* Set the current step to SENDING_PKG_ID and proceed to send the package ID */
            progressTracker.setCurrentStep(SENDING_PKG_ID);

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);
            repositoryNodeSession.send(pkgId);

            /* Set the current step to RECEIVING_PKG_INFO and proceed to retrieve the package info */
            progressTracker.setCurrentStep(RECEIVING_PKG_INFO);

            final StateAndRef<PkgOfferState> pkgStateAndRef = receiveAndValidatePkgState(repositoryNodeSession,
                    repositoryNode, price);

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final PkgLicenseState pkgLicenseState = new PkgLicenseState(pkgStateAndRef, buyer);
            final Command<PkgLicenseContract.Commands.BuyPkg> txCommand = new Command<>(
                    new PkgLicenseContract.Commands.BuyPkg(), ImmutableList.of(buyer.getOwningKey(),
                    repositoryNode.getOwningKey()));
            TransactionBuilder txBuilder = new TransactionBuilder(notary);

            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), price.getToken());
            if(cashBalance.getQuantity() < price.getQuantity())
                throw new IllegalArgumentException(missingCash);

            Pair<TransactionBuilder, List<PublicKey>> txKeysPair =
                    CashUtils.generateSpend(getServiceHub(), txBuilder, price, getOurIdentityAndCert(), repositoryNode);
            final TransactionBuilder tx = txKeysPair.getFirst();
            tx.addOutputState(pkgLicenseState, PkgLicenseContract.ID).addCommand(txCommand);

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
                    ImmutableList.of(repositoryNodeSession)));

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new FinalityFlow(fullySignedTx, ImmutableList.of(repositoryNodeSession)));
        }

        @Suspendable
        private StateAndRef<PkgOfferState> receiveAndValidatePkgState(FlowSession repositoryNodeSession,
                                                                      Party repositoryNode,
                                                                      Amount<Currency> price) throws FlowException {
            /* Retrieve the package info and verify that a single PkgOfferState has been received */
            final List<StateAndRef<PkgOfferState>> receivedObjects =
                    subFlow(new ReceiveStateAndRefFlow<>(repositoryNodeSession));

            /* Set the current step to VERIFYING_PKG_INFO and proceed to verify the received PkgOfferState */
            progressTracker.setCurrentStep(VERIFYING_PKG_INFO);

            return requireThat(require -> {
                require.using(receivedTooMuchStates, receivedObjects.size() == 1);
                final StateAndRef<PkgOfferState> pkgStateAndRef = receivedObjects.get(0);
                final PkgOfferState pkgOfferState = pkgStateAndRef.getState().getData();
                require.using(requestedPkgErr, pkgId.equals(pkgOfferState.getLinearId()));
                require.using(priceMismatch, price.equals(pkgOfferState.getPrice()));
                require.using(repositoryNodeMismatch, repositoryNode.equals(pkgOfferState.getRepositoryNode()));
                return pkgStateAndRef;
            });
        }
    }

    @InitiatedBy(PkgBuyerInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<SignedTransaction> {

        private final FlowSession buyerSession;

        private final Step AWAITING_PKG_ID        = new Step(BuyPkgFlowUtils.AWAITING_PKG_ID);
        private final Step VERIFYING_RCV_DATA     = new Step(BuyPkgFlowUtils.VERIFYING_RCV_DATA);
        private final Step SENDING_PKG_INFO       = new Step(BuyPkgFlowUtils.SENDING_PKG_INFO);
        private final Step FINALISING_TRANSACTION = new Step(BuyPkgFlowUtils.FINALISING_TRANSACTION) {
            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };
        private final Step SENDING_CASH_TO_AUTHOR = new Step(BuyPkgFlowUtils.SENDING_CASH_TO_AUTHOR);

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
         * function.
         */
        private final ProgressTracker progressTracker = new ProgressTracker(
                AWAITING_PKG_ID,
                VERIFYING_RCV_DATA,
                SENDING_PKG_INFO,
                FINALISING_TRANSACTION,
                SENDING_CASH_TO_AUTHOR
        );

        /**
         * Constructor of the flow initiated by the PkgBuyerInitiation class
         * @param buyerSession session with the buyer that want to purchase a package
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

            /* Set the current step to AWAITING_PKG_ID and proceed to call */
            progressTracker.setCurrentStep(AWAITING_PKG_ID);

            final StateAndRef<PkgOfferState> pkgStateAndRef = buyerSession.receive(UniqueIdentifier.class).unwrap(data -> {
                /* Set the current step to VERIFYING_RCV_DATA and proceed to verify the received data */
                progressTracker.setCurrentStep(VERIFYING_RCV_DATA);

                /* Query for unconsumed linear state for given linear ID */
                QueryCriteria.LinearStateQueryCriteria queryCriteria =
                        new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(data.getId()),
                        null, Vault.StateStatus.UNCONSUMED);
                final List<StateAndRef<PkgOfferState>> lst = getServiceHub().getVaultService()
                        .queryBy(PkgOfferState.class, queryCriteria).getStates();
                if(lst.size() == 0)
                    throw new NonExistentPkgException(data);

                return lst.get(0);
            });

            /* Set the current step to SENDING_PKG_INFO and proceed to send the requested package info */
            progressTracker.setCurrentStep(SENDING_PKG_INFO);

            subFlow(new SendStateAndRefFlow(buyerSession, ImmutableList.of(pkgStateAndRef)));

            /* Check and Sign the transaction, get the hash value of the obtained transaction */
            final SignTxFlow signTxFlow = new SignTxFlow(buyerSession, SignTransactionFlow.Companion.tracker(),
                    pkgStateAndRef.getState().getData().getPrice());
            final SecureHash txId = subFlow(signTxFlow).getId();

            /*
             * Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow
             * Receive the transaction that will be stored in the vault and compare it's hash value
             * with the previously saved hash; if it's the same, proceed storing the transaction
             */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            SignedTransaction stx = subFlow(new ReceiveFinalityFlow(buyerSession, txId));

            /*
             * Set the current step to SENDING_CASH_TO_AUTHOR and starts a sub flow to send the amount that belongs
             * to the author of the package sold
             */
            progressTracker.setCurrentStep(SENDING_CASH_TO_AUTHOR);

            PkgLicenseState pkgLicenseState = stx.getTx().outputsOfType(PkgLicenseState.class).get(0);
            subFlow(new IssueCashToDevFlow.IssueCashToDevInitiator(pkgLicenseState,
                    pkgLicenseState.getPkgLicensed().getState().getData().getAuthor()));

            return stx;
        }
    }
}
