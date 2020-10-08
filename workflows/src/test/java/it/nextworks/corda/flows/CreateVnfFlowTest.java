package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
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

import java.util.Currency;
import java.util.List;
import java.util.Locale;

import static it.nextworks.corda.flows.CreateVnfFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CreateVnfFlowTest {

    private MockNetwork mockNetwork;
    private StartedMockNode devNodeTest;
    private StartedMockNode repositoryNodeTest;

    /** Attributes used to construct transactions inside tests */
    private static final String name            = "testVNF";
    private static final String description     = "test";
    private static final String serviceType     = "testService";
    private static final String version         = "1.0";
    private static final String requirements    = "n.b";
    private static final String resources       = "n.b";
    private static final String link            = "https://www.nextworks.it/";
    private static final Amount<Currency> price = new Amount<>(1,
            Currency.getInstance(Locale.ITALY));

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
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(name, description, serviceType,
                version, requirements, resources, link, link, price);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(name, description, serviceType,
                version, requirements, resources, link, link, price);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(devNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(name, description, serviceType,
                version, requirements, resources, link, link, price);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputVnf() throws Exception {
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(name, description, serviceType,
                version, requirements, resources, link, link, price);
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
        CreateVnfFlow.DevInitiation flow = new CreateVnfFlow.DevInitiation(name, description, serviceType,
                version, requirements, resources, link, link, price);
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
        assertEquals(recordedState.getName(), name);
        assertEquals(recordedState.getDescription(), description);
        assertEquals(recordedState.getServiceType(), serviceType);
        assertEquals(recordedState.getVersion(), version);
        assertEquals(recordedState.getRequirements(), requirements);
        assertEquals(recordedState.getResources(), resources);
        assertEquals(recordedState.getImageLink(), link);
        assertEquals(recordedState.getRepositoryLink(), link);
        assertEquals(recordedState.getRepositoryHash(), link.hashCode());
        assertEquals(recordedState.getRepositoryLink(), link);
        assertEquals(recordedState.getPrice(), price);
        assertEquals(recordedState.getAuthor(), devNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(recordedState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }
}
