package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgOfferUtils;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.concurrent.CordaFuture;
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

import java.math.BigDecimal;
import java.util.List;

import static it.nextworks.corda.flows.GetPkgsFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetFilteredPkgsFlowTest {
    private MockNetwork mockNetwork;
    private StartedMockNode devNodeTest;
    private StartedMockNode buyerNodeTest;
    private StartedMockNode repositoryNodeTest;

    /** Build a mock network composed by a developer Node, a buyer Node and the repository Node and a Notary */
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
        buyerNodeTest = mockNetwork.createPartyNode(CordaX500Name.parse(buyerX500Name));

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

    /** Function used to generate a transaction that will output a PkgOfferState */
    private PkgOfferState generatePkgOfferState() throws Exception {
        ProductOfferingPrice poPrice = new ProductOfferingPrice(PkgOfferUtils.testPoId, PkgOfferUtils.testLink, PkgOfferUtils.testDescription,
                PkgOfferUtils.testIsBundle, PkgOfferUtils.testLastUpdate, PkgOfferUtils.testLifecycleStatus,
                PkgOfferUtils.testPoName, PkgOfferUtils.testPercentage, PkgOfferUtils.testPriceType,
                PkgOfferUtils.testRecChargePeriodLength, PkgOfferUtils.testRecChargePeriodType,
                PkgOfferUtils.testVersion, PkgOfferUtils.testPrice, PkgOfferUtils.testQuantity,
                PkgOfferUtils.testValidFor);
        RegisterPkgFlow.DevInitiation flow = new RegisterPkgFlow.DevInitiation(PkgOfferUtils.testName,
                PkgOfferUtils.testDescription, PkgOfferUtils.testVersion, PkgOfferUtils.testPkgInfoId,
                PkgOfferUtils.testLink, PkgOfferUtils.testPkgType, poPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTransaction = future.get();
        return signedTransaction.getTx().outputsOfType(PkgOfferState.class).get(0);
    }

    @Test
    public void retrieveStateByLinearIdFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState = generatePkgOfferState();
        UniqueIdentifier linearId = pkgOfferState.getLinearId();
        generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setLinearId(linearId.getId()).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 1);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), linearId, pkgOfferState.getPoPrice());
    }

    @Test
    public void retrieveStatesByNameFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        PkgOfferState pkgOfferState1 = generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setName(PkgOfferUtils.testName).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 2);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(1), pkgOfferState1.getLinearId(),
                pkgOfferState1.getPoPrice());
    }

    @Test
    public void retrieveStatesByDescriptionFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        PkgOfferState pkgOfferState1 = generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setDescription(PkgOfferUtils.testDescription).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 2);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(1), pkgOfferState1.getLinearId(),
                pkgOfferState1.getPoPrice());
    }

    @Test
    public void retrieveStatesByVersionFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        PkgOfferState pkgOfferState1 = generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setVersion(PkgOfferUtils.testVersion).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 2);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(1), pkgOfferState1.getLinearId(),
                pkgOfferState1.getPoPrice());
    }

    @Test
    public void retrieveStatesByValueFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        PkgOfferState pkgOfferState1 = generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setValue(BigDecimal.valueOf(2).setScale(2, BigDecimal.ROUND_HALF_EVEN)).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 2);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(1), pkgOfferState1.getLinearId(),
                pkgOfferState1.getPoPrice());
    }

    @Test
    public void retrieveStatesByUnitFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        PkgOfferState pkgOfferState1 = generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder().setUnit("EUR").build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 2);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(1), pkgOfferState1.getLinearId(),
                pkgOfferState1.getPoPrice());
    }

    @Test
    public void retrieveStatesByLinearIdAndNameFromTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        UniqueIdentifier linearId = pkgOfferState.getLinearId();
        generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setLinearId(linearId.getId()).setName(PkgOfferUtils.testName).build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 1);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), linearId,
                pkgOfferState.getPoPrice());
    }

    @Test
    public void noStatesAvailableForThisQueryInTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState  = generatePkgOfferState();
        UniqueIdentifier linearId = pkgOfferState.getLinearId();
        generatePkgOfferState();

        GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation flow =
                new GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation(new QueryBuilder()
                        .setName(PkgOfferUtils.testName).setUnit("USD").build());
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 0);
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
}
