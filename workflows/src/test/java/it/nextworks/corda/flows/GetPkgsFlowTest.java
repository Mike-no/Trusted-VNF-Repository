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

import java.util.List;

import static it.nextworks.corda.flows.GetPkgsFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetPkgsFlowTest {

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
    public void storedPkgOfferCanBeViewedInTheMarketplace() throws Exception {
        generateFeeAgreementState();
        PkgOfferState pkgOfferState = generatePkgOfferState();

        GetPkgsFlow.GetPkgsInfoInitiation flow = new GetPkgsFlow.GetPkgsInfoInitiation();
        CordaFuture<List<PkgOfferState>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<PkgOfferState> pkgOfferStateListList = future.get();

        assert (pkgOfferStateListList.size() == 1);
        checkPkgOfferStateCorrectness(pkgOfferStateListList.get(0), pkgOfferState.getLinearId(),
                pkgOfferState.getPoPrice());
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
