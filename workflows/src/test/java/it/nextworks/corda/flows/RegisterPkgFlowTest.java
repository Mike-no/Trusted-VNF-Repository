package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgOfferUtils;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static it.nextworks.corda.flows.RegisterPkgFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class RegisterPkgFlowTest {

    private MockNetwork mockNetwork;
    private StartedMockNode devNodeTest;
    private StartedMockNode repositoryNodeTest;

    /** Build a mock network composed by a developer Node, the repository Node and a Notary */
    @Before
    public void setup() {
        mockNetwork = new MockNetwork(new MockNetworkParameters(
                ImmutableList.of(
                        TestCordapp.findCordapp(cordAppContractsPkg),
                        TestCordapp.findCordapp(cordAppFlowsPkg)))
                .withNotarySpecs(ImmutableList.of(
                        new MockNetworkNotarySpec(CordaX500Name.parse(notaryX500Name)))));
        devNodeTest = mockNetwork.createPartyNode(CordaX500Name.parse(devX500Name));
        repositoryNodeTest = mockNetwork.createPartyNode(CordaX500Name.parse(repositoryX500Name));

        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /** Function used to generate a transaction that will output a FeeAgreementState */
    private void generateFeeAgreementState() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get();
    }

    private RegisterPkgFlow.DevInitiation createFlow() {
        ProductOfferingPrice poPrice = new ProductOfferingPrice(PkgOfferUtils.testPoId, PkgOfferUtils.testLink, PkgOfferUtils.testDescription,
                PkgOfferUtils.testIsBundle, PkgOfferUtils.testLastUpdate, PkgOfferUtils.testLifecycleStatus,
                PkgOfferUtils.testPoName, PkgOfferUtils.testPercentage, PkgOfferUtils.testPriceType,
                PkgOfferUtils.testRecChargePeriodLength, PkgOfferUtils.testRecChargePeriodType,
                PkgOfferUtils.testVersion, PkgOfferUtils.testPrice, PkgOfferUtils.testQuantity,
                PkgOfferUtils.testValidFor);
        return new RegisterPkgFlow.DevInitiation(PkgOfferUtils.testName, PkgOfferUtils.testDescription,
                PkgOfferUtils.testVersion, PkgOfferUtils.testPkgInfoId, PkgOfferUtils.testLink,
                PkgOfferUtils.testPkgType, poPrice);
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        generateFeeAgreementState();
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        generateFeeAgreementState();
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(devNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        generateFeeAgreementState();
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputPkg() throws Exception {
        generateFeeAgreementState();
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        PkgOfferState pkgOfferState = ((PkgOfferState) signedTx.getTx().getOutput(0));
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId());
            assert (recordedTx.getInputs().size() == 0);
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);
            ContractState contractState = txOutputs.get(0).getData();
            assert (contractState instanceof PkgOfferState);

            PkgOfferState recordedState = (PkgOfferState) contractState;
            checkPkgOfferStateCorrectness(recordedState, pkgOfferState.getLinearId(), pkgOfferState.getPoPrice());
        }
    }

    @Test
    public void flowRecordsTheCorrectPkgInBothPartiesVaults() throws Exception {
        generateFeeAgreementState();
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        PkgOfferState pkgOfferState = ((PkgOfferState) signedTx.getTx().getOutput(0));
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            node.transaction(() -> {
                List<StateAndRef<PkgOfferState>> pkgs = node.getServices().getVaultService()
                        .queryBy(PkgOfferState.class).getStates();
                assertEquals(pkgs.size(), 1);

                PkgOfferState recordedState = pkgs.get(0).getState().getData();
                checkPkgOfferStateCorrectness(recordedState, pkgOfferState.getLinearId(), pkgOfferState.getPoPrice());

                return null;
            });
        }
    }

    private void checkPkgOfferStateCorrectness(@NotNull PkgOfferState recordedState, @NotNull UniqueIdentifier pkgId,
                                               @NotNull ProductOfferingPrice poPrice) {
        assertEquals(recordedState.getLinearId(), pkgId);
        assertEquals(recordedState.getName(), PkgOfferUtils.testName);
        assertEquals(recordedState.getDescription(), PkgOfferUtils.testDescription);
        assertEquals(recordedState.getVersion(), PkgOfferUtils.testVersion);
        assertEquals(recordedState.getPkgInfoId(), PkgOfferUtils.testPkgInfoId);
        assertEquals(recordedState.getImageLink(), PkgOfferUtils.testLink);
        assertEquals(recordedState.getPoPrice(), poPrice);
        assertEquals(recordedState.getPkgType(), PkgOfferUtils.testPkgType);
        assertEquals(recordedState.getAuthor(), devNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(recordedState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }

    @Test
    public void feeAgreementMustBeEstablished() {
        RegisterPkgFlow.DevInitiation flow = createFlow();
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            future.get();
        } catch(Exception exception) {
            assert exception.getMessage()
                    .equals("it.nextworks.corda.flows.RegisterPkgFlow$NotExistingAgreementException: "
                    + notExistingAgreement);
        }
    }
}
