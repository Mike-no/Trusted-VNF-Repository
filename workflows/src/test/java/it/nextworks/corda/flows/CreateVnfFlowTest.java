package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.TransactionState;
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

import static it.nextworks.corda.flows.CreateVnfFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateVnfFlowTest {

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

        repositoryNodeTest.registerInitiatedFlow(CreateVnfFlow.RepositoryNodeAcceptor.class);

        mockNetwork.runNetwork();
    }

    @After
    public void tearDown() {
        mockNetwork.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(devNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputVnf() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId());
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);

            VnfState recordedState = (VnfState) txOutputs.get(0).getData();
            checkStateCorrectness(recordedState);
        }
    }

    @Test
    public void flowRecordsTheCorrectVnfInBothPartiesVaults() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            node.transaction(() -> {
                List<StateAndRef<VnfState>> vnfs = node.getServices().getVaultService()
                        .queryBy(VnfState.class).getStates();
                assertEquals(vnfs.size(), 1);

                VnfState recordedState = vnfs.get(0).getState().getData();
                checkStateCorrectness(recordedState);

                return null;
            });
        }
    }

    private void checkStateCorrectness(@NotNull VnfState recordedState) {
        assertEquals(recordedState.getName(), testName);
        assertEquals(recordedState.getDescription(), testDescription);
        assertEquals(recordedState.getServiceType(), testServiceType);
        assertEquals(recordedState.getVersion(), testVersion);
        assertEquals(recordedState.getRequirements(), testRequirements);
        assertEquals(recordedState.getResources(), testResources);
        assertEquals(recordedState.getImageLink(), testLink);
        assertEquals(recordedState.getRepositoryLink(), testLink);
        assertEquals(recordedState.getRepositoryHash(), testLink.hashCode());
        assertEquals(recordedState.getPrice(), testPrice);
        assertEquals(recordedState.getAuthor(), devNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(recordedState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }
}
