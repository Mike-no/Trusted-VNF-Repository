package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.dsl.TransactionDSL;
import net.corda.testing.node.MockServices;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static it.nextworks.corda.contracts.PkgOfferUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class DeletePkgContractTest {

    /** Simulate a Corda Network composed by two nodes: a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(ImmutableList.of(cordAppContractsPkg),
                    devTest, repositoryNodeTest);

    private PkgOfferState createPkgOfferState() {
        ProductOfferingPrice poPrice = new ProductOfferingPrice(PkgOfferUtils.testPoId, PkgOfferUtils.testLink, PkgOfferUtils.testDescription,
                PkgOfferUtils.testIsBundle, PkgOfferUtils.testLastUpdate, PkgOfferUtils.testLifecycleStatus,
                PkgOfferUtils.testPoName, PkgOfferUtils.testPercentage, PkgOfferUtils.testPriceType,
                PkgOfferUtils.testRecChargePeriodLength, PkgOfferUtils.testRecChargePeriodType,
                PkgOfferUtils.testVersion, PkgOfferUtils.testPrice, PkgOfferUtils.testQuantity,
                PkgOfferUtils.testValidFor);
        return new PkgOfferState(PkgOfferUtils.testId, PkgOfferUtils.testName,
                PkgOfferUtils.testDescription, PkgOfferUtils.testVersion, PkgOfferUtils.testPkgInfoId,
                PkgOfferUtils.testLink, PkgOfferUtils.testPkgType, poPrice, devTest.getParty(),
                repositoryNodeTest.getParty());
    }

    /**
     * Function used to generate a transaction that will output a PkgOfferState that will
     * be used in a DeletePkg transaction as input
     * @param tx transaction that will output a PkgOfferState
     */
    private void generateInputPkgOfferState(@NotNull TransactionDSL<?> tx) {
        PkgOfferState pkgOfferState = createPkgOfferState();
        tx.output(PkgOfferContract.ID, toBeDeleted, pkgOfferState);
        tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                new PkgOfferContract.Commands.RegisterPkg());
    }

    /** Test that a transaction must include the DeletePkg command */
    @Test
    public void transactionMustIncludeDeleteCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeDeleted);
                tx.input(inputPkg.getRef());
                tx.fails();
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.DeletePkg());

                return tx.verifies();
            });
            return null;
        }));
    }

    /** Test that a transaction must have only one input of type PkgOfferState */
    @Test
    public void transactionMustHaveOnlyOneInputOfTypePkgOfferState() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                tx.output(PkgOfferContract.ID, createPkgOfferState());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.DeletePkg());

                return tx.failsWith(deletePkgInputErr);
            });
            return null;
        }));
    }

    /** Test that a transaction must not have output */
    @Test
    public void transactionMustNotHaveOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeDeleted);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, createPkgOfferState());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.DeletePkg());

                return tx.failsWith(deletePkgOutputErr);
            });
            return null;
        }));
    }

    /** Test that the <author> must sign the transaction */
    @Test
    public void authorMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeDeleted);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.DeletePkg());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryNode> must sign the transaction */
    @Test
    public void repositoryNodeMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeDeleted);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(devTest.getPublicKey()),
                        new PkgOfferContract.Commands.DeletePkg());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}
