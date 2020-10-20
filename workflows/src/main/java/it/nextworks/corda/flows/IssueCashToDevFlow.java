package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.PkgLicenseState;
import it.nextworks.corda.states.PkgOfferState;
import kotlin.Pair;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.flows.IssueCashToDevFlowUtils.*;

public class IssueCashToDevFlow {

    @InitiatingFlow
    public static class IssueCashToDevInitiator extends FlowLogic<SignedTransaction> {

        private final PkgLicenseState pkgLicenseState;
        private final Party developer;

        private final Step SENDING_LICENSE        = new Step(IssueCashToDevFlowUtils.SENDING_LICENSE);
        private final Step GENERATING_TRANSACTION = new Step(IssueCashToDevFlowUtils.GENERATING_TRANSACTION);
        private final Step SIGNING_TRANSACTION    = new Step(IssueCashToDevFlowUtils.SIGNING_TRANSACTION);
        private final Step FINALISING_TRANSACTION = new Step(IssueCashToDevFlowUtils.FINALISING_TRANSACTION) {
            @Override
            public ProgressTracker childProgressTracker() { return FinalityFlow.Companion.tracker(); }
        };

        /**
         * The progress tracker checkpoints each stage of the flow and outputs the specified messages when each
         * checkpoint is reached in the code. See the 'progressTracker.currentStep' expressions within the call()
         * function.
         */
        private final ProgressTracker progressTracker = new ProgressTracker(
                SENDING_LICENSE,
                GENERATING_TRANSACTION,
                SIGNING_TRANSACTION,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the Initiating flow class.
         * @param developer Party to send money to
         */
        public IssueCashToDevInitiator(PkgLicenseState pkgLicenseState, Party developer){
            this.pkgLicenseState = pkgLicenseState;
            this.developer = developer;
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

            /* Set the current step to SENDING_LICENSE and proceed to send the license to the developer */
            progressTracker.setCurrentStep(SENDING_LICENSE);

            FlowSession devNodeSession = initiateFlow(developer);
            devNodeSession.send(pkgLicenseState);

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            Amount<Currency> amount = pkgLicenseState.getPkgLicensed().getState().getData().getPrice();
            Amount<Currency> toIssue =
                    Amount.fromDecimal(amount.toDecimal().divide(BigDecimal.valueOf(100), 4,
                            RoundingMode.UNNECESSARY).multiply(BigDecimal.valueOf(90)).setScale(2,
                            BigDecimal.ROUND_HALF_EVEN), amount.getToken());

            TransactionBuilder txBuilder = new TransactionBuilder(notary);
            Pair<TransactionBuilder, List<PublicKey>> txKeysPair =
                    CashUtils.generateSpend(getServiceHub(), txBuilder, toIssue, getOurIdentityAndCert(), developer);
            final TransactionBuilder tx = txKeysPair.getFirst();

            /* Set the current step to SIGNING_TRANSACTION and proceed to sign the latter */
            progressTracker.setCurrentStep(SIGNING_TRANSACTION);

            List<PublicKey> keysToSign = txKeysPair.getSecond();
            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(tx, keysToSign);

            /* Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            ImmutableList<FlowSession> sessionForFinality;
            if(getServiceHub().getMyInfo().isLegalIdentity(developer))
                sessionForFinality = ImmutableList.of();
            else
                sessionForFinality = ImmutableList.of(devNodeSession);

            return subFlow(new FinalityFlow(signedTx, sessionForFinality));
        }
    }

    @InitiatedBy(IssueCashToDevInitiator.class)
    public static class DeveloperNodeAcceptor extends FlowLogic<Void> {

        private final FlowSession repositoryNodeSession;

        /**
         * Constructor of the flow initiated by the IssueCashToDevInitiator class
         * @param repositoryNodeSession session with the repositoryNode that want to issue cash to the developer
         */
        public DeveloperNodeAcceptor(FlowSession repositoryNodeSession) {
            this.repositoryNodeSession = repositoryNodeSession;
        }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            repositoryNodeSession.receive(PkgLicenseState.class).unwrap(data -> {
                PkgOfferState soldPkg = data.getPkgLicensed().getState().getData();
                UniqueIdentifier id = soldPkg.getLinearId();
                QueryCriteria.LinearStateQueryCriteria queryCriteria =
                        new QueryCriteria.LinearStateQueryCriteria(null,
                                ImmutableList.of(id.getId()), null, Vault.StateStatus.UNCONSUMED);
                final List<StateAndRef<PkgOfferState>> lst = getServiceHub().getVaultService()
                        .queryBy(PkgOfferState.class, queryCriteria).getStates();
                if(lst.size() == 0)
                    throw new BuyPkgFlow.NonExistentPkgException(id);

                return null;
            });

            if(!getServiceHub().getMyInfo().isLegalIdentity(repositoryNodeSession.getCounterparty()))
                subFlow(new ReceiveFinalityFlow(repositoryNodeSession));

            return null;
        }
    }
}
