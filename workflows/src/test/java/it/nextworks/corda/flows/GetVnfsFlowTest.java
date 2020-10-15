package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfState;
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

import static it.nextworks.corda.flows.GetVnfsFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class GetVnfsFlowTest {

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
        repositoryNodeTest.registerInitiatedFlow(CreateVnfFlow.RepositoryNodeAcceptor.class);

        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    /** Function used to generate a transaction that will output a VnfState */
    private VnfState generateVnfState() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(CreateVnfFlowUtils.testName,
                CreateVnfFlowUtils.testDescription, CreateVnfFlowUtils.testServiceType,
                CreateVnfFlowUtils.testVersion, CreateVnfFlowUtils.testRequirements,
                CreateVnfFlowUtils.testResources, CreateVnfFlowUtils.testLink,
                CreateVnfFlowUtils.testLink, CreateVnfFlowUtils.testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTransaction = future.get();

        return signedTransaction.getTx().outputsOfType(VnfState.class).get(0);
    }

    @Test
    public void storedVnfStateCanBeViewedInTheMarketplace() throws Exception {
        VnfState vnfState = generateVnfState();

        GetVnfsFlow.GetVnfInfoInitiation flow = new GetVnfsFlow.GetVnfInfoInitiation();
        CordaFuture<List<GetVnfsFlow.VnfInfo>> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        List<GetVnfsFlow.VnfInfo> vnfInfoList = future.get();
        assert (vnfInfoList.size() == 1);
        GetVnfsFlow.VnfInfo vnfInfo = vnfInfoList.get(0);
        checkVnfInfoCorrectness(vnfInfo, vnfState.getLinearId());
    }

    private void checkVnfInfoCorrectness(@NotNull GetVnfsFlow.VnfInfo vnfInfo, @NotNull UniqueIdentifier vnfId) {
        assertEquals(vnfInfo.getVnfId(), vnfId);
        assertEquals(vnfInfo.getName(), CreateVnfFlowUtils.testName);
        assertEquals(vnfInfo.getDescription(), CreateVnfFlowUtils.testDescription);
        assertEquals(vnfInfo.getServiceType(), CreateVnfFlowUtils.testServiceType);
        assertEquals(vnfInfo.getVersion(), CreateVnfFlowUtils.testVersion);
        assertEquals(vnfInfo.getImageLink(), CreateVnfFlowUtils.testLink);
        assertEquals(vnfInfo.getPrice(), CreateVnfFlowUtils.testPrice);
        assertEquals(vnfInfo.getAuthor(), devNodeTest.getInfo().getLegalIdentities().get(0));
    }
}
