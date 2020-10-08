package it.nextworks.corda.flows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import it.nextworks.corda.states.VnfState;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
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

import static it.nextworks.corda.flows.CreateVnfFlowUtils.*;
import static net.corda.testing.core.ExpectKt.expect;
import static net.corda.testing.core.ExpectKt.expectEvents;
import static net.corda.testing.driver.Driver.driver;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DriverBasedCreateVnfFlowTest {

    /** Define a user Node and a repository Node legal names */
    private final TestIdentity devNodeTest        =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private final User notaryUser = new User(notaryUsername, notaryPsw, ImmutableSet.of("ALL"));

    @Test
    public void nodeTest() {
        /** Build up a network with three nodes: a dev node, a repository node and a notary node */
        driver(new DriverParameters()
                .withIsDebug(true)
                .withStartNodesInProcess(true)
                .withCordappsForAllNodes(ImmutableList.of(
                        TestCordapp.findCordapp(cordAppContractsPkg),
                        TestCordapp.findCordapp(cordAppFlowsPkg)))
                .withNotarySpecs(ImmutableList.of(
                        new NotarySpec(CordaX500Name.parse(notaryX500Name),
                                true, Arrays.asList(notaryUser), VerifierType.InMemory, null))), dsl -> {
            List<CordaFuture<NodeHandle>> handleFutures = ImmutableList.of(
                    dsl.startNode(new NodeParameters().withProvidedName(CordaX500Name.parse(devX500Name))),
                    dsl.startNode(new NodeParameters().withProvidedName(CordaX500Name.parse(repositoryX500Name)))
            );

            try {
                NodeHandle devHandle = handleFutures.get(0).get();
                NodeHandle repositoryHandle = handleFutures.get(1).get();

                /**
                 * Assert that the developer node and the repository
                 * node can communicate (see each other in the network)
                 */
                assertEquals(repositoryHandle.getRpc()
                                .wellKnownPartyFromX500Name(devNodeTest.getName()).getName(),
                        devNodeTest.getName());
                assertEquals(devHandle.getRpc()
                                .wellKnownPartyFromX500Name(repositoryNodeTest.getName()).getName(),
                        repositoryNodeTest.getName());

                /** Register the observer object to track the dev's vault and the repository's vault */
                Observable<Vault.Update<VnfState>> devVaultUpdates =
                        devHandle.getRpc().vaultTrack(VnfState.class).getUpdates();
                Observable<Vault.Update<VnfState>> repositoryVaultUpdates =
                        repositoryHandle.getRpc().vaultTrack(VnfState.class).getUpdates();

                /** Start the creation flow and verify that the vnf state has been stored in the vault of each node */
                devHandle.getRpc().startFlowDynamic(CreateVnfFlow.DevInitiation.class,
                        testName, testDescription, testServiceType, testVersion, testRequirements,
                        testResources, testLink, testLink, testPrice).getReturnValue().get();

                Class<Vault.Update<VnfState>> vnfVaultUpdateClass =
                        (Class<Vault.Update<VnfState>>)(Class<?>)Vault.Update.class;

                expectEvents(devVaultUpdates, true, () ->
                        expect(vnfVaultUpdateClass, update -> true, update -> {
                            VnfState recordedState =
                                    update.getProduced().iterator().next().getState().getData();
                            checkStateCorrectness(recordedState, devHandle.getNodeInfo().getLegalIdentities().get(0),
                                    repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                            return null;
                        })
                );
                expectEvents(repositoryVaultUpdates, true, () ->
                        expect(vnfVaultUpdateClass, update -> true, update -> {
                            VnfState recordedState =
                                    update.getProduced().iterator().next().getState().getData();
                            checkStateCorrectness(recordedState, devHandle.getNodeInfo().getLegalIdentities().get(0),
                                    repositoryHandle.getNodeInfo().getLegalIdentities().get(0));

                            return null;
                        })
                );
            } catch(Exception e) {
                throw new RuntimeException(integrationTestEx, e);
            }

            return null;
        });
    }

    private void checkStateCorrectness(@NotNull VnfState recordedState, Party author, Party repositoryNode) {
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
}
