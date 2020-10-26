package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgOfferContract;
import it.nextworks.corda.states.FeeAgreementState;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import static it.nextworks.corda.flows.RegisterPkgFlowUtils.*;

public class RegisterPkgFlow {
    /**
     * This exception will be thrown if the developer hasn't already established
     * a fee agreement with the Repository Node.
     */
    public static class NotExistingAgreementException extends FlowException {
        public NotExistingAgreementException() { super(notExistingAgreement); }
    }

    /**
     * This exception will be thrown if an error occur while sending the http
     * request to the 5g-catalogue.
     */
    public static class CannotPerformPkgRegister extends FlowException {
        public CannotPerformPkgRegister(String msg) { super(msg); }
    }


    @InitiatingFlow
    @StartableByRPC
    public static class DevInitiation extends FlowLogic<SignedTransaction> {

        private final String name;
        private final String description;
        private final String version;
        private final String pkgInfoId;
        private final String imageLink;
        private final PkgOfferState.PkgType pkgType;
        private final ProductOfferingPrice poPrice;

        private final String httpRequest;

        private final Step SENDING_PATH_REQUEST   = new Step(RegisterPkgFlowUtils.SENDING_PATH_REQUEST);
        private final Step GENERATING_TRANSACTION = new Step(RegisterPkgFlowUtils.GENERATING_TRANSACTION);
        private final Step VERIFYING_TRANSACTION  = new Step(RegisterPkgFlowUtils.VERIFYING_TRANSACTION);
        private final Step SIGNING_TRANSACTION    = new Step(RegisterPkgFlowUtils.SIGNING_TRANSACTION);
        private final Step GATHERING_SIGNS        = new Step(RegisterPkgFlowUtils.GATHERING_SIGNS){
            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };
        private final Step FINALISING_TRANSACTION = new Step(RegisterPkgFlowUtils.FINALISING_TRANSACTION) {
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
                SENDING_PATH_REQUEST,
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
         * @param pkgType        type of the package (VNF or PNF) to build in the transaction
         * @param poPrice        product offering price of the package to build in the transaction
         */
        public DevInitiation(String name,
                             String description,
                             String version,
                             String pkgInfoId,
                             String imageLink,
                             PkgOfferState.PkgType pkgType,
                             ProductOfferingPrice poPrice,
                             String httpRequest) {
            this.name           = name;
            this.description    = description;
            this.version        = version;
            this.pkgInfoId      = pkgInfoId;
            this.imageLink      = imageLink;
            this.pkgType        = pkgType;
            this.poPrice        = poPrice;

            this.httpRequest    = httpRequest;
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

            /* Set the current step to SENDING_PATH_REQUEST and proceed to send the base path of the http request */
            progressTracker.setCurrentStep(SENDING_PATH_REQUEST);

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);
            repositoryNodeSession.send(httpRequest);

            /* Set the current step to GENERATING_TRANSACTION and proceed to build the latter */
            progressTracker.setCurrentStep(GENERATING_TRANSACTION);

            final PkgOfferState pkgOfferState = new PkgOfferState(new UniqueIdentifier(), name, description,
                    version, pkgInfoId, imageLink, pkgType, poPrice, author, repositoryNode);
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

        private final Step AWAITING_PATH_REQUEST  = new Step(RegisterPkgFlowUtils.AWAITING_PATH_REQUEST);
        private final Step VERIFYING_RCV_DATA     = new Step(RegisterPkgFlowUtils.VERIFYING_RCV_DATA);
        private final Step FINALISING_TRANSACTION = new Step(RegisterPkgFlowUtils.FINALISING_TRANSACTION) {
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
                AWAITING_PATH_REQUEST,
                VERIFYING_RCV_DATA,
                FINALISING_TRANSACTION
        );

        /**
         * Constructor of the flow initiated by the DevInitiation class
         * @param devSession session with the developer that want to submit his package
         */
        public RepositoryNodeAcceptor(FlowSession devSession) {
            this.devSession = devSession;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            class SignTxFlow extends SignTransactionFlow {

                private String httpRequest;

                private SignTxFlow(FlowSession devSession, ProgressTracker progressTracker, String httpRequest) {
                    super(devSession, progressTracker);
                    this.httpRequest = httpRequest;
                }

                /**
                 * Override the checkTransaction function to define the behaviour of the
                 * repositoryNode when accepts new package
                 */
                @Override
                protected void checkTransaction(@NotNull SignedTransaction stx)
                        throws NotExistingAgreementException, CannotPerformPkgRegister {
                    ContractState output = stx.getTx().getOutputs().get(0).getData();
                    if(!(output instanceof PkgOfferState))
                        throw new IllegalArgumentException(notPkgStateErr);

                    /* Verify that a fee agreement exists between the developer and the repository node */

                    QueryCriteria.VaultQueryCriteria queryCriteria =
                            new QueryCriteria.VaultQueryCriteria()
                                    .withExactParticipants(ImmutableList.of(
                                            devSession.getCounterparty(), getOurIdentity()));
                    List<StateAndRef<FeeAgreementState>> lst =
                            getServiceHub().getVaultService().queryBy(FeeAgreementState.class, queryCriteria).getStates();
                    if(lst.isEmpty())
                        throw new NotExistingAgreementException();

                    /* Verify that the package has been on-boarded on the 5g-catalogue */

                    PkgOfferState pkgOfferState = (PkgOfferState)output;
                    if(pkgOfferState.getPkgType().equals(PkgOfferState.PkgType.VNF))
                        httpRequest += "vnfpkgm/v1/vnf_packages/";
                    else
                        httpRequest += "nsd/v1/pnf_descriptors/";
                    httpRequest += pkgOfferState.getPkgInfoId();

                    final Request request = new Request.Builder().url(httpRequest)
                            .addHeader("Accept", "application/json").build();
                    try {
                        Response httpResponse = new OkHttpClient().newCall(request).execute();
                        if(!httpResponse.isSuccessful())
                            throw new CannotPerformPkgRegister(httpResponse.body().string());
                        httpResponse.close();
                    } catch(IOException e) {
                        throw new CannotPerformPkgRegister(e.getMessage());
                    }
                }
            }

            /* Set the current step to AWAITING_PATH_REQUEST and proceed to call */
            progressTracker.setCurrentStep(AWAITING_PATH_REQUEST);

            String httpRequest = devSession.receive(String.class).unwrap(data -> {
                /* Set the current step to VERIFYING_RCV_DATA and proceed to verify the received data */
                progressTracker.setCurrentStep(VERIFYING_RCV_DATA);

                try {
                    new URL(data);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException(notBasePathErr);
                }

                return data;
            });

            /* Check and Sign the transaction, get the hash value of the obtained transaction */
            final SignTxFlow signTxFlow = new SignTxFlow(devSession,
                    SignTransactionFlow.Companion.tracker(), httpRequest);
            final SecureHash txId = subFlow(signTxFlow).getId();

            /*
             * Set the current step to FINALISING_TRANSACTION and starts a finalising sub-flow
             * Receive the transaction that will be stored in the vault and compare it's hash value
             * with the previously saved hash; if it's the same, proceed storing the transaction
             */
            progressTracker.setCurrentStep(FINALISING_TRANSACTION);

            return subFlow(new ReceiveFinalityFlow(devSession, txId));
        }
    }
}
