package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.FeeAgreementState;
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

import static it.nextworks.corda.flows.EstablishFeeAgreementFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EstablishFeeAgreementFlowTest {

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

        repositoryNodeTest.registerInitiatedFlow(RegisterPkgFlow.RepositoryNodeAcceptor.class);

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
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(devNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasNoInputsAndASingleOutputPkg() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId());
            assert (recordedTx.getInputs().size() == 0);
            List<TransactionState<ContractState>> txOutputs = recordedTx.getTx().getOutputs();
            assert (txOutputs.size() == 1);
            ContractState contractState = txOutputs.get(0).getData();
            assert (contractState instanceof FeeAgreementState);

            FeeAgreementState feeAgreementState = (FeeAgreementState)contractState;
            checkFeeAgreementStateCorrectness(feeAgreementState);
        }
    }

    @Test
    public void flowRecordsTheCorrectPkgInBothPartiesVaults() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation flow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            node.transaction(() -> {
                List<StateAndRef<FeeAgreementState>> fees = node.getServices().getVaultService()
                        .queryBy(FeeAgreementState.class).getStates();
                assertEquals(fees.size(), 1);

                FeeAgreementState feeAgreementState = fees.get(0).getState().getData();
                checkFeeAgreementStateCorrectness(feeAgreementState);

                return null;
            });
        }
    }

    private void checkFeeAgreementStateCorrectness(@NotNull FeeAgreementState feeAgreementState) {
        assertEquals(feeAgreementState.getDeveloper(), devNodeTest.getInfo().getLegalIdentities().get(0));
        assertEquals(feeAgreementState.getRepositoryNode(), repositoryNodeTest.getInfo().getLegalIdentities().get(0));
    }

    @Test
    public void agreementMustBeEstablishedOneTime() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation firstFlow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> firstAgreement = devNodeTest.startFlow(firstFlow);

        mockNetwork.runNetwork();
        firstAgreement.get();

        EstablishFeeAgreementFlow.DevInitiation secondFlow = new EstablishFeeAgreementFlow.DevInitiation(15);
        CordaFuture<SignedTransaction> secondAgreement = devNodeTest.startFlow(secondFlow);

        try {
            mockNetwork.runNetwork();
            secondAgreement.get();
        } catch(Exception exception) {
            assert exception.getMessage()
                    .equals("it.nextworks.corda.flows.EstablishFeeAgreementFlow$AlreadyEstablishedAgreementException: "
                            + AlreadyEstablishedFee);
        }
    }

    @Test
    public void agreementBetweenPartyMustBeReached() throws Exception {
        EstablishFeeAgreementFlow.DevInitiation firstFlow = new EstablishFeeAgreementFlow.DevInitiation(9);
        CordaFuture<SignedTransaction> firstAgreement = devNodeTest.startFlow(firstFlow);

        try {
            mockNetwork.runNetwork();
            firstAgreement.get();
        } catch(Exception exception) {
            assert exception.getMessage()
                    .equals("net.corda.core.flows.FlowException: " +
                            "java.lang.IllegalArgumentException: Failed requirement: " + tooHighFee);
        }
    }
}
