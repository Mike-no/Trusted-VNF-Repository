package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.nextworks.corda.states.VnfLicenseState;
import it.nextworks.corda.states.VnfState;
import net.corda.core.concurrent.CordaFuture;
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

import java.util.Arrays;
import java.util.List;

import static it.nextworks.corda.flows.BuyVnfFlowUtils.buyerX500Name;
import static it.nextworks.corda.flows.BuyVnfFlowUtils.cordAppFinance;
import static it.nextworks.corda.flows.CreateVnfFlowUtils.*;
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
                                true, Arrays.asList(notaryUser), VerifierType.InMemory, null))), dsl -> {
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
                assertEquals(repositoryHandle.getRpc().wellKnownPartyFromX500Name(devNodeTest.getName()).getName(),
                        devNodeTest.getName());
                assertEquals(devHandle.getRpc().wellKnownPartyFromX500Name(repositoryNodeTest.getName()).getName(),
                        repositoryNodeTest.getName());
                assertEquals(repositoryHandle.getRpc().wellKnownPartyFromX500Name(buyerNodeTest.getName()).getName(),
                        buyerNodeTest.getName());
                assertEquals(buyerHandle.getRpc().wellKnownPartyFromX500Name(repositoryNodeTest.getName()).getName(),
                        repositoryNodeTest.getName());
                assertEquals(devHandle.getRpc().wellKnownPartyFromX500Name(buyerNodeTest.getName()).getName(),
                        buyerNodeTest.getName());
                assertEquals(buyerHandle.getRpc().wellKnownPartyFromX500Name(devNodeTest.getName()).getName(),
                        devNodeTest.getName());

                /*
                 * Register the observer objects to track the dev's vault and the repository's vault
                 * Register the observer objects to track the buyer's vault and the repository's vault
                 */
                Observable<Vault.Update<VnfState>> devVaultUpdates =
                        devHandle.getRpc().vaultTrack(VnfState.class).getUpdates();
                Observable<Vault.Update<VnfState>> repositoryVnfVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(VnfState.class).getUpdates();
                Observable<Vault.Update<VnfLicenseState>> buyerVaultUpdates =
                        buyerHandle.getRpc().vaultTrack(VnfLicenseState.class).getUpdates();
                Observable<Vault.Update<VnfLicenseState>> repositoryLicenseVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(VnfLicenseState.class).getUpdates();

                /* Start the creation flow and verify that the vnf state has been stored in the vault of each node */
                SignedTransaction signedTransaction =
                        devHandle.getRpc().startFlowDynamic(CreateVnfFlow.DevInitiation.class,
                        testName, testDescription, testServiceType, testVersion, testRequirements,
                        testResources, testLink, testLink, testPrice).getReturnValue().get();
                UniqueIdentifier vnfId = ((VnfState)signedTransaction.getTx().getOutput(0)).getLinearId();

                Class<Vault.Update<VnfState>> vnfVaultUpdateClass =
                        (Class<Vault.Update<VnfState>>)(Class<?>)Vault.Update.class;

                checkVaultsAfterCreateVnf(devVaultUpdates, vnfVaultUpdateClass, vnfId, devHandle, repositoryHandle);
                checkVaultsAfterCreateVnf(repositoryVnfVaultUpdates, vnfVaultUpdateClass, vnfId, devHandle, repositoryHandle);

                /* Start the get vnfs flow to get the vnfId of a the vnf pkg as will be shown in the marketplace */
                List<GetVnfsFlow.VnfInfo> vnfInfoList =
                        buyerHandle.getRpc().startFlowDynamic(GetVnfsFlow.GetVnfInfoInitiation.class).getReturnValue().get();
                assert (vnfInfoList.size() == 1);
                GetVnfsFlow.VnfInfo vnfInfo = vnfInfoList.get(0);
                UniqueIdentifier retrievedVnfId = vnfInfo.getVnfId();

                /* Start the buy flow and verify that the license state has been stored in the vault of each node */
                buyerHandle.getRpc().startFlowDynamic(SelfIssueCashFlow.class, testPrice).getReturnValue().get();
                buyerHandle.getRpc().startFlowDynamic(BuyVnfFlow.VnfBuyerInitiation.class, retrievedVnfId, testPrice)
                        .getReturnValue().get();

                Class<Vault.Update<VnfLicenseState>> vnfLicenseUpdateClass =
                        (Class<Vault.Update<VnfLicenseState>>)(Class<?>)Vault.Update.class;

                checkVaultsAfterBuyVnf(buyerVaultUpdates, vnfLicenseUpdateClass, retrievedVnfId, devHandle,
                        buyerHandle, repositoryHandle);
                checkVaultsAfterBuyVnf(repositoryLicenseVaultUpdates, vnfLicenseUpdateClass, retrievedVnfId, devHandle,
                        buyerHandle, repositoryHandle);
            } catch(Exception e) {
                throw new RuntimeException(integrationTestEx, e);
            }

            return null;
        });
    }

    private void checkVaultsAfterCreateVnf(@NotNull Observable<Vault.Update<VnfState>> observer,
                                           @NotNull Class<Vault.Update<VnfState>> vnfVaultUpdateClass,
                                           @NotNull UniqueIdentifier vnfId, @NotNull NodeHandle devHandle,
                                           @NotNull NodeHandle repositoryHandle) {
        expectEvents(observer, true, () ->
                expect(vnfVaultUpdateClass, update -> true, update -> {
                    VnfState recordedState = update.getProduced().iterator().next().getState().getData();
                    checkVnfCorrectness(recordedState, vnfId, devHandle.getNodeInfo().getLegalIdentities().get(0),
                            repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                    return null;
                })
        );
    }

    private void checkVaultsAfterBuyVnf(@NotNull Observable<Vault.Update<VnfLicenseState>> observer,
                                        @NotNull Class<Vault.Update<VnfLicenseState>> vnfLicenseUpdateClass,
                                        @NotNull UniqueIdentifier vnfId, @NotNull NodeHandle devHandle,
                                        @NotNull NodeHandle buyerHandle, @NotNull NodeHandle repositoryHandle) {
        expectEvents(observer, true, () ->
                expect(vnfLicenseUpdateClass, update -> true, update -> {
                    VnfLicenseState recordedState = update.getProduced().iterator().next().getState().getData();
                    VnfState vnfState = recordedState.getVnfLicensed().getState().getData();
                    checkVnfCorrectness(vnfState, vnfId, devHandle.getNodeInfo().getLegalIdentities().get(0),
                            repositoryHandle.getNodeInfo().getLegalIdentities().get(0));
                    checkLicenseCorrectness(recordedState, buyerHandle.getNodeInfo().getLegalIdentities().get(0),
                            repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                    return null;
                })
        );
    }

    private void checkVnfCorrectness(@NotNull VnfState recordedState, @NotNull UniqueIdentifier vnfId,
                                       @NotNull Party author, @NotNull Party repositoryNode) {
        assertEquals(recordedState.getLinearId(), vnfId);
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
        assertEquals(recordedState.getAuthor(), author);
        assertEquals(recordedState.getRepositoryNode(), repositoryNode);
    }

    private void checkLicenseCorrectness(@NotNull VnfLicenseState recordedState, @NotNull Party buyer,
                                         @NotNull Party repositoryNode) {
        assertEquals(recordedState.getRepositoryLink(), CreateVnfFlowUtils.testLink);
        assertEquals(recordedState.getRepositoryHash(), CreateVnfFlowUtils.testLink.hashCode());
        assertEquals(recordedState.getBuyer(), buyer);
        assertEquals(recordedState.getRepositoryNode(), repositoryNode);
    }
}
