package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.nextworks.corda.contracts.PkgOfferUtils;
import it.nextworks.corda.states.FeeAgreementState;
import it.nextworks.corda.states.PkgLicenseState;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.driver.DriverParameters;
import net.corda.testing.driver.NodeHandle;
import net.corda.testing.driver.NodeParameters;
import net.corda.testing.driver.VerifierType;
import net.corda.testing.node.NotarySpec;
import net.corda.testing.node.TestCordapp;
import net.corda.testing.node.User;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import rx.Observable;

import java.util.Currency;
import java.util.List;
import java.util.Objects;

import static it.nextworks.corda.flows.DriverBasedFlowsTestUtils.*;
import static net.corda.testing.core.ExpectKt.expect;
import static net.corda.testing.core.ExpectKt.expectEvents;
import static net.corda.testing.driver.Driver.driver;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DriverBasedFlowsTest {

    /** Define a user Node, a repository Node and a Buyer Node legal names */
    private final TestIdentity devNodeTest        =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private final TestIdentity buyerNodeTest      =
            new TestIdentity(CordaX500Name.parse(buyerX500Name));
    private final User notaryUser = new User(notaryUsername, notaryPsw, ImmutableSet.of("ALL"));

    @Test
    public void nodeTest() {
        /* Build up a network with three nodes: a dev node, a repository node and a notary node */
        driver(new DriverParameters()
                .withIsDebug(true)
                .withStartNodesInProcess(true)
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp(cordAppContractsPkg),
                        TestCordapp.findCordapp(cordAppFlowsPkg),
                        TestCordapp.findCordapp(cordAppFinance)))
                .withNotarySpecs(ImmutableList.of(
                        new NotarySpec(CordaX500Name.parse(notaryX500Name),
                                true, ImmutableList.of(notaryUser), VerifierType.InMemory, null))), dsl -> {
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(CordaX500Name.parse(devX500Name))),
                    dsl.startNode(new NodeParameters().withProvidedName(CordaX500Name.parse(repositoryX500Name))),
                    dsl.startNode(new NodeParameters().withProvidedName(CordaX500Name.parse(buyerX500Name)))
            );

            try {
                NodeHandle devHandle = handleFutures.get(0).get();
                NodeHandle repositoryHandle = handleFutures.get(1).get();
                NodeHandle buyerHandle = handleFutures.get(2).get();

                /*
                 * Assert that the developer node and the repository
                 * node can communicate (see each other in the network)
                 * Assert that the buyer node and the repository
                 * node can communicate (see each other in the network)
                 * Assert that the buyer node and the developer
                 * node can communicate (see each other in the network)
                 */
                assertEquals(Objects.requireNonNull(repositoryHandle.getRpc()
                        .wellKnownPartyFromX500Name(devNodeTest.getName())).getName(), devNodeTest.getName());
                assertEquals(Objects.requireNonNull(devHandle.getRpc()
                        .wellKnownPartyFromX500Name(repositoryNodeTest.getName())).getName(),
                        repositoryNodeTest.getName());
                assertEquals(Objects.requireNonNull(repositoryHandle.getRpc()
                        .wellKnownPartyFromX500Name(buyerNodeTest.getName())).getName(), buyerNodeTest.getName());
                assertEquals(Objects.requireNonNull(buyerHandle.getRpc()
                        .wellKnownPartyFromX500Name(repositoryNodeTest.getName())).getName(),
                        repositoryNodeTest.getName());
                assertEquals(Objects.requireNonNull(devHandle.getRpc()
                        .wellKnownPartyFromX500Name(buyerNodeTest.getName())).getName(), buyerNodeTest.getName());
                assertEquals(Objects.requireNonNull(buyerHandle.getRpc()
                        .wellKnownPartyFromX500Name(devNodeTest.getName())).getName(), devNodeTest.getName());

                /*
                 * Register the observer objects to track the dev's vault and the repository's vault (FeeAgreementState)
                 * Register the observer objects to track the dev's vault and the repository's vault (PkgOfferState)
                 * Register the observer objects to track the buyer's vault and the repository's vault
                 */
                Observable<Vault.Update<FeeAgreementState>> devFeeVaultUpdates =
                        devHandle.getRpc().vaultTrack(FeeAgreementState.class).getUpdates();
                Observable<Vault.Update<FeeAgreementState>> repositoryFeeVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(FeeAgreementState.class).getUpdates();
                Observable<Vault.Update<PkgOfferState>> devVaultUpdates =
                        devHandle.getRpc().vaultTrack(PkgOfferState.class).getUpdates();
                Observable<Vault.Update<PkgOfferState>> repositoryPkgVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(PkgOfferState.class).getUpdates();
                Observable<Vault.Update<PkgLicenseState>> buyerVaultUpdates =
                        buyerHandle.getRpc().vaultTrack(PkgLicenseState.class).getUpdates();
                Observable<Vault.Update<PkgLicenseState>> repositoryLicenseVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(PkgLicenseState.class).getUpdates();

                /* Start the fee agreement flow and verify that the fee state has been stored in the vault of nodes */
                devHandle.getRpc().startFlowDynamic(EstablishFeeAgreementFlow.DevInitiation.class, 15)
                        .getReturnValue().get();

                @SuppressWarnings("unchecked")
                Class<Vault.Update<FeeAgreementState>> feeVaultUpdateClass =
                        (Class<Vault.Update<FeeAgreementState>>)(Class<?>)Vault.Update.class;

                checkVaultsAfterFeeAgreement(devFeeVaultUpdates, feeVaultUpdateClass,
                        devHandle.getNodeInfo().getLegalIdentities().get(0),
                        repositoryHandle.getNodeInfo().getLegalIdentities().get(0));
                checkVaultsAfterFeeAgreement(repositoryFeeVaultUpdates, feeVaultUpdateClass,
                        devHandle.getNodeInfo().getLegalIdentities().get(0),
                        repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                /* Start the creation flow and verify that the pkg state has been stored in the vault of each node */
                ProductOfferingPrice poPrice = new ProductOfferingPrice(PkgOfferUtils.testPoId, PkgOfferUtils.testLink,
                        PkgOfferUtils.testDescription, PkgOfferUtils.testIsBundle, PkgOfferUtils.testLastUpdate,
                        PkgOfferUtils.testLifecycleStatus, PkgOfferUtils.testPoName, PkgOfferUtils.testPercentage,
                        PkgOfferUtils.testPriceType, PkgOfferUtils.testRecChargePeriodLength,
                        PkgOfferUtils.testRecChargePeriodType, PkgOfferUtils.testVersion, PkgOfferUtils.testPrice,
                        PkgOfferUtils.testQuantity, PkgOfferUtils.testValidFor);
                SignedTransaction signedTransaction =
                        devHandle.getRpc().startFlowDynamic(RegisterPkgFlow.DevInitiation.class,
                            PkgOfferUtils.testName, PkgOfferUtils.testDescription, PkgOfferUtils.testVersion,
                            PkgOfferUtils.testPkgInfoId, PkgOfferUtils.testLink, PkgOfferUtils.testPkgType,
                                poPrice).getReturnValue().get();
                PkgOfferState pkgOfferState = (PkgOfferState)signedTransaction.getTx().getOutput(0);
                UniqueIdentifier pkgId = pkgOfferState.getLinearId();
                Amount<Currency> price = pkgOfferState.getPrice();

                @SuppressWarnings("unchecked")
                Class<Vault.Update<PkgOfferState>> pkgVaultUpdateClass =
                        (Class<Vault.Update<PkgOfferState>>)(Class<?>)Vault.Update.class;

                checkVaultsAfterCreatePkg(devVaultUpdates, pkgVaultUpdateClass, pkgId,
                        poPrice, devHandle, repositoryHandle);
                checkVaultsAfterCreatePkg(repositoryPkgVaultUpdates, pkgVaultUpdateClass, pkgId,
                        poPrice, devHandle, repositoryHandle);

                /* Start the get pkgs flow to get the pkgId of a the pkg as will be shown in the marketplace */
                List<PkgOfferState> pkgOfferStateList =
                        buyerHandle.getRpc().startFlowDynamic(GetPkgsFlow.GetPkgsInfoInitiation.class)
                                .getReturnValue().get();

                List<PkgOfferState> pkgOfferStateList1 =
                        buyerHandle.getRpc().startFlowDynamic(GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation.class,
                                new GetPkgsFlowUtils.QueryBuilder().setName(PkgOfferUtils.testName).build())
                                .getReturnValue().get();

                assert (pkgOfferStateList.size() == pkgOfferStateList1.size());
                assert (pkgOfferStateList.size() == 1);
                PkgOfferState retrievedPkgOfferState = pkgOfferStateList.get(0);
                UniqueIdentifier retrievedPkgId = retrievedPkgOfferState.getLinearId();

                /* Start the buy flow and verify that the license state has been stored in the vault of each node */
                buyerHandle.getRpc().startFlowDynamic(SelfIssueCashFlow.class, price)
                        .getReturnValue().get();
                buyerHandle.getRpc().startFlowDynamic(BuyPkgFlow.PkgBuyerInitiation.class, retrievedPkgId,
                        price).getReturnValue().get();

                @SuppressWarnings("unchecked")
                Class<Vault.Update<PkgLicenseState>> pkgLicenseUpdateClass =
                        (Class<Vault.Update<PkgLicenseState>>)(Class<?>)Vault.Update.class;

                checkVaultsAfterBuyPkg(buyerVaultUpdates, pkgLicenseUpdateClass, pkgId, poPrice, devHandle,
                        buyerHandle, repositoryHandle);
                checkVaultsAfterBuyPkg(repositoryLicenseVaultUpdates, pkgLicenseUpdateClass, pkgId, poPrice, devHandle,
                        buyerHandle, repositoryHandle);

                /* Register the observer objects to track the dev's vault and the repository's vault (UpdatePkg) */
                Observable<Vault.Update<PkgOfferState>> devVaultUpdatesAfterUpdate =
                        devHandle.getRpc().vaultTrack(PkgOfferState.class).getUpdates();
                Observable<Vault.Update<PkgOfferState>> repositoryPkgVaultUpdatesAfterUpdate =
                        repositoryHandle.getRpc().vaultTrack(PkgOfferState.class).getUpdates();

                /*
                Start the update pkg flow and verify that the updated pkg state has been stored in
                the vault of each node
                */
                devHandle.getRpc().startFlowDynamic(UpdatePkgFlow.DevInitiation.class,
                        pkgId, PkgOfferUtils.testNameUpdate, PkgOfferUtils.testDescriptionUpdate,
                        PkgOfferUtils.testVersionUpdate, PkgOfferUtils.testLinkUpdate, poPrice).getReturnValue().get();

                checkVaultsAfterPkgUpdate(devVaultUpdatesAfterUpdate, pkgVaultUpdateClass, pkgOfferState);
                checkVaultsAfterPkgUpdate(repositoryPkgVaultUpdatesAfterUpdate, pkgVaultUpdateClass, pkgOfferState);

                /*
                Start the delete flow and verify that the pkg has been deleted from
                the marketplace (signed as CONSUMED in the vault)
                */
                devHandle.getRpc().startTrackedFlowDynamic(DeletePkgFlow.DevInitiation.class, pkgId)
                        .getReturnValue().get();

                List<PkgOfferState> pkgOfferStateListAfterDelete =
                        buyerHandle.getRpc().startFlowDynamic(GetPkgsFlow.GetPkgsInfoInitiation.class)
                                .getReturnValue().get();
                assert (pkgOfferStateListAfterDelete.isEmpty());
            } catch(Exception e) {
                throw new RuntimeException(integrationTestEx, e);
            }

            return null;
        });
    }

    private void checkVaultsAfterFeeAgreement(@NotNull Observable<Vault.Update<FeeAgreementState>> observer,
                                              @NotNull Class<Vault.Update<FeeAgreementState>> feeVaultUpdateClass,
                                              @NotNull Party developer,
                                              @NotNull Party repositoryNode) {
        expectEvents(observer, true, () ->
                expect(feeVaultUpdateClass, update -> true, update -> {
                    FeeAgreementState feeAgreementState = update.getProduced().iterator().next().getState().getData();
                    assertEquals(feeAgreementState.getDeveloper(), developer);
                    assertEquals(feeAgreementState.getRepositoryNode(), repositoryNode);

                    return null;
                })
        );
    }

    private void checkVaultsAfterCreatePkg(@NotNull Observable<Vault.Update<PkgOfferState>> observer,
                                           @NotNull Class<Vault.Update<PkgOfferState>> pkgVaultUpdateClass,
                                           @NotNull UniqueIdentifier pkgId,
                                           @NotNull ProductOfferingPrice poPrice,
                                           @NotNull NodeHandle devHandle,
                                           @NotNull NodeHandle repositoryHandle) {
        expectEvents(observer, true, () ->
                expect(pkgVaultUpdateClass, update -> true, update -> {
                    PkgOfferState recordedState = update.getProduced().iterator().next().getState().getData();
                    checkPkgOfferStateCorrectness(recordedState, pkgId, poPrice,
                            devHandle.getNodeInfo().getLegalIdentities().get(0),
                            repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                    return null;
                })
        );
    }

    private void checkVaultsAfterBuyPkg(@NotNull Observable<Vault.Update<PkgLicenseState>> observer,
                                        @NotNull Class<Vault.Update<PkgLicenseState>> pkgLicenseUpdateClass,
                                        @NotNull UniqueIdentifier pkgId,
                                        @NotNull ProductOfferingPrice poPrice,
                                        @NotNull NodeHandle devHandle,
                                        @NotNull NodeHandle buyerHandle,
                                        @NotNull NodeHandle repositoryHandle) {
        expectEvents(observer, true, () ->
                expect(pkgLicenseUpdateClass, update -> true, update -> {
                    PkgLicenseState recordedState = update.getProduced().iterator().next().getState().getData();
                    PkgOfferState pkgOfferState = recordedState.getPkgLicensed().getState().getData();
                    checkPkgOfferStateCorrectness(pkgOfferState, pkgId, poPrice,
                            devHandle.getNodeInfo().getLegalIdentities().get(0),
                            repositoryHandle.getNodeInfo().getLegalIdentities().get(0));
                    assertEquals(recordedState.getBuyer(), buyerHandle.getNodeInfo().getLegalIdentities().get(0));

                    return null;
                })
        );
    }

    private void checkPkgOfferStateCorrectness(@NotNull PkgOfferState recordedState,
                                               @NotNull UniqueIdentifier pkgId,
                                               @NotNull ProductOfferingPrice poPrice,
                                               @NotNull Party author,
                                               @NotNull Party repositoryNode) {
        assertEquals(recordedState.getLinearId(), pkgId);
        assertEquals(recordedState.getName(), PkgOfferUtils.testName);
        assertEquals(recordedState.getDescription(), PkgOfferUtils.testDescription);
        assertEquals(recordedState.getVersion(), PkgOfferUtils.testVersion);
        assertEquals(recordedState.getPkgInfoId(), PkgOfferUtils.testPkgInfoId);
        assertEquals(recordedState.getImageLink(), PkgOfferUtils.testLink);
        assertEquals(recordedState.getPoPrice(), poPrice);
        assertEquals(recordedState.getPkgType(), PkgOfferUtils.testPkgType);
        assertEquals(recordedState.getAuthor(), author);
        assertEquals(recordedState.getRepositoryNode(), repositoryNode);
    }

    private void checkVaultsAfterPkgUpdate(@NotNull Observable<Vault.Update<PkgOfferState>> observer,
                                           @NotNull Class<Vault.Update<PkgOfferState>> pkgVaultUpdateClass,
                                           @NotNull PkgOfferState oldPkgOfferState) {
        expectEvents(observer, true, () ->
                expect(pkgVaultUpdateClass, update -> true, update -> {
                    PkgOfferState newPkgOfferState = update.getProduced().iterator().next().getState().getData();
                    checkUpdatedPkgStateCorrectness(oldPkgOfferState, newPkgOfferState);

                    return null;
                })
        );
    }

    private void checkUpdatedPkgStateCorrectness(PkgOfferState oldPkgOfferState, PkgOfferState newPkgOfferState) {
        assertEquals(oldPkgOfferState.getLinearId(), newPkgOfferState.getLinearId());
        assertEquals(newPkgOfferState.getName(), PkgOfferUtils.testNameUpdate);
        assertEquals(newPkgOfferState.getDescription(), PkgOfferUtils.testDescriptionUpdate);
        assertEquals(newPkgOfferState.getVersion(), PkgOfferUtils.testVersionUpdate);
        assertEquals(oldPkgOfferState.getPkgInfoId(), newPkgOfferState.getPkgInfoId());
        assertEquals(newPkgOfferState.getImageLink(), PkgOfferUtils.testLinkUpdate);
        assertEquals(oldPkgOfferState.getPoPrice(), newPkgOfferState.getPoPrice());
        assertEquals(oldPkgOfferState.getPkgType(), newPkgOfferState.getPkgType());
        assertEquals(oldPkgOfferState.getAuthor(), newPkgOfferState.getAuthor());
        assertEquals(oldPkgOfferState.getRepositoryNode(), newPkgOfferState.getRepositoryNode());
    }
}
