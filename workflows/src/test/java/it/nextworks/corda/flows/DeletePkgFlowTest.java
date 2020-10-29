package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.contracts.PkgOfferUtils;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.List;

import static it.nextworks.corda.flows.DeletePkgFlowUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DeletePkgFlowTest {

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

    /**
     * Function used to generate a transaction that will output a PkgOfferState that will
     * be used in a PkgLicenseState transaction as component of a PkgLicenseState
     */
    private UniqueIdentifier generatePkgOfferState() throws Exception {
        ProductOfferingPrice poPrice = new ProductOfferingPrice(PkgOfferUtils.testPoId, PkgOfferUtils.testLink,
                PkgOfferUtils.testDescription, PkgOfferUtils.testIsBundle, PkgOfferUtils.testLastUpdate,
                PkgOfferUtils.testLifecycleStatus, PkgOfferUtils.testPoName, PkgOfferUtils.testPercentage,
                PkgOfferUtils.testPriceType, PkgOfferUtils.testRecChargePeriodLength, PkgOfferUtils.testRecChargePeriodType,
                PkgOfferUtils.testVersion, PkgOfferUtils.testPrice, PkgOfferUtils.testQuantity,
                PkgOfferUtils.testValidFor);
        RegisterPkgFlow.DevInitiation flow = new RegisterPkgFlow.DevInitiation(PkgOfferUtils.testName,
                PkgOfferUtils.testDescription, PkgOfferUtils.testVersion, PkgOfferUtils.testPkgInfoId,
                PkgOfferUtils.testLink, PkgOfferUtils.testPkgType, poPrice);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTransaction = future.get();

        return signedTransaction.getTx().outputsOfType(PkgOfferState.class).get(0).getLinearId();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {
        generateFeeAgreementState();
        UniqueIdentifier linearId = generatePkgOfferState();

        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(linearId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(repositoryNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        generateFeeAgreementState();
        UniqueIdentifier linearId = generatePkgOfferState();

        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(linearId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(devNodeTest.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        generateFeeAgreementState();
        UniqueIdentifier linearId = generatePkgOfferState();

        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(linearId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }

    @Test
    public void recordedTransactionHasOneInputAndNoOutput() throws Exception {
        generateFeeAgreementState();
        UniqueIdentifier linearId = generatePkgOfferState();

        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(linearId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        SignedTransaction signedTx = future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            SignedTransaction recordedTx = node.getServices().getValidatedTransactions()
                    .getTransaction(signedTx.getId());
            assert (recordedTx.getInputs().size() == 1);
            assert (recordedTx.getTx().getOutputs().isEmpty());
        }
    }

    @Test
    public void flowDeletesPkgInBothPartiesVaults() throws Exception {
        generateFeeAgreementState();
        UniqueIdentifier linearId = generatePkgOfferState();

        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(linearId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        mockNetwork.runNetwork();

        future.get();
        for(StartedMockNode node : ImmutableList.of(devNodeTest, repositoryNodeTest)) {
            node.transaction(() -> {
                QueryCriteria.VaultQueryCriteria queryCriteria = new QueryCriteria.VaultQueryCriteria()
                        .withStatus(Vault.StateStatus.UNCONSUMED);
                List<StateAndRef<PkgOfferState>> pkgs = node.getServices().getVaultService()
                        .queryBy(PkgOfferState.class, queryCriteria).getStates();
                assertEquals(pkgs.size(), 0);

                return null;
            });
        }
    }

    @Test
    public void pkgMustExist() throws Exception {
        generateFeeAgreementState();
        generatePkgOfferState();

        UniqueIdentifier pkgId = new UniqueIdentifier();
        DeletePkgFlow.DevInitiation flow = new DeletePkgFlow.DevInitiation(pkgId);
        CordaFuture<SignedTransaction> future = devNodeTest.startFlow(flow);

        try {
            mockNetwork.runNetwork();
            future.get();
        } catch(Exception exception) {
            assert exception.getMessage()
                    .equals("it.nextworks.corda.flows.DeletePkgFlow$NonExistentPkgException: "
                    + nonExistentPkg + pkgId);
        }
    }
}
