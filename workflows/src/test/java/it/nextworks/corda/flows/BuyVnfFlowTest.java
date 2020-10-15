package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfLicenseState;
import it.nextworks.corda.states.VnfState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.finance.Currencies;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.node.*;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.contracts.VnfUtils.buyVnfOutputCashErr;
import static it.nextworks.corda.flows.BuyVnfFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuyVnfFlowTest {

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
                        TestCordapp.findCordapp(cordAppFlowsPkg),
                        TestCordapp.findCordapp(cordAppFinance)))
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

    /**
     * Function used to generate a transaction that will output a VnfState that will
     * be used in a VnfLicenseState transaction as component of a VnfLicenseState
     */
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

    private void issueCash(Amount<Currency> amount) {
        SelfIssueCashFlow flow = new SelfIssueCashFlow(amount);
        buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(buyerNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowReturnsCommittedTransaction() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get().verifyRequiredSignatures();
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(buyerNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionIsCorrectlyFormed() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(buyerNodeTest, repositoryNodeTest)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId());
            WireTransaction tx = recordedTx.getTx();

            assert (tx.getInputs().size() > 0);

            int vnfLicenseStateCount = 0;
            int cashOutputStateCount = 0;
            for(ContractState output : tx.getOutputStates()) {
                if(output instanceof VnfLicenseState)
                    vnfLicenseStateCount++;
                else if(output instanceof Cash.State)
                    cashOutputStateCount++;
                else
                    throw new IllegalArgumentException(buyVnfOutputCashErr);
            }
            assert (cashOutputStateCount > 0);
            assert (vnfLicenseStateCount == 1);

            final VnfLicenseState vnfLicenseState = tx.outputsOfType(VnfLicenseState.class).get(0);
            final VnfState savedVnfState = vnfLicenseState.getVnfLicensed().getState().getData();
            checkVnfCorrectness(savedVnfState, vnfState.getLinearId());
            checkLicenseCorrectness(vnfLicenseState);
        }
    }

    @Test
    public void flowRecordsTheCorrectVnfLicenseInBothPartiesVaults() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(vnfState.getPrice());

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get();
        for(StartedMockNode node : ImmutableList.of(buyerNodeTest, repositoryNodeTest)) {
            node.transaction(() -> {
                List<StateAndRef<VnfLicenseState>> vnfLicenseStates = node.getServices().getVaultService()
                        .queryBy(VnfLicenseState.class).getStates();
                assertEquals(vnfLicenseStates.size(), 1);

                final VnfLicenseState vnfLicenseState = vnfLicenseStates.get(0).getState().getData();
                final VnfState savedVnfState = vnfLicenseState.getVnfLicensed().getState().getData();
                checkVnfCorrectness(savedVnfState, vnfState.getLinearId());
                checkLicenseCorrectness(vnfLicenseState);

                return null;
            });
        }
    }

    private void checkVnfCorrectness(@NotNull VnfState recordedState, @NotNull UniqueIdentifier vnfId) {
        assertEquals(recordedState.getLinearId(), vnfId);
        assertEquals(recordedState.getName(), CreateVnfFlowUtils.testName);
        assertEquals(recordedState.getDescription(), CreateVnfFlowUtils.testDescription);
        assertEquals(recordedState.getServiceType(), CreateVnfFlowUtils.testServiceType);
        assertEquals(recordedState.getVersion(), CreateVnfFlowUtils.testVersion);
        assertEquals(recordedState.getRequirements(), CreateVnfFlowUtils.testRequirements);
        assertEquals(recordedState.getResources(), CreateVnfFlowUtils.testResources);
        assertEquals(recordedState.getImageLink(), CreateVnfFlowUtils.testLink);
        assertEquals(recordedState.getRepositoryLink(), CreateVnfFlowUtils.testLink);
        assertEquals(recordedState.getRepositoryHash(), CreateVnfFlowUtils.testLink.hashCode());
        assertEquals(recordedState.getPrice(), CreateVnfFlowUtils.testPrice);
        assertEquals(recordedState.getAuthor(), devNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(recordedState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }

    private void checkLicenseCorrectness(@NotNull VnfLicenseState recordedState) {
        assertEquals(recordedState.getRepositoryLink(), CreateVnfFlowUtils.testLink);
        assertEquals(recordedState.getRepositoryHash(), CreateVnfFlowUtils.testLink.hashCode());
        assertEquals(recordedState.getBuyer(), buyerNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(recordedState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }

    @Test
    public void borrowerMustHaveCashInRightCurrency() throws Exception {
        VnfState vnfState = generateVnfState();
        issueCash(Currencies.DOLLARS(1));

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            future.get();
        } catch(Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: " + missingCash);
        }
    }

    @Test
    public void borrowerMustHaveEnoughCashInRightCurrency() throws Exception {
        VnfState vnfState = generateVnfState();

        BuyVnfFlow.VnfBuyerInitiation flow =
                new BuyVnfFlow.VnfBuyerInitiation(vnfState.getLinearId(), vnfState.getPrice());
        CordaFuture<SignedTransaction> future = buyerNodeTest.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            future.get();
        } catch(Exception exception) {
            assert exception.getMessage().equals("java.lang.IllegalArgumentException: " + missingCash);
        }
    }
}
