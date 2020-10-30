package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.ProductOfferingPrice;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.dsl.TransactionDSL;
import net.corda.testing.node.MockServices;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static it.nextworks.corda.contracts.PkgOfferUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

/** Test PkgOfferContract class in cases where the command used in the transaction is UpdatePkg */
public class UpdatePkgContractTest {

    /** Simulate a Corda Network composed by two nodes: a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(ImmutableList.of(cordAppContractsPkg),
                    devTest, repositoryNodeTest);

    public ProductOfferingPrice createProductOfferingPrice() {
        return new ProductOfferingPrice(testPoId, testLink, testDescription, testIsBundle, testLastUpdate,
                testLifecycleStatus, testPoName, testPercentage, testPriceType, testRecChargePeriodLength,
                testRecChargePeriodType, testVersion, testPrice, testQuantity, testValidFor);
    }

    private void generateInputPkgOfferState(@NotNull TransactionDSL<?> tx) {
        PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                testPkgInfoId, testLink, testPkgType, createProductOfferingPrice(),
                devTest.getParty(), repositoryNodeTest.getParty());
        tx.output(PkgOfferContract.ID, toBeUpdated, pkgOfferState);
        tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                new PkgOfferContract.Commands.RegisterPkg());
    }

    /** Test that a transaction must include the UpdatePkg command */
    @Test
    public void transactionMustIncludeUpdateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.fails();
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

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
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updatePkgInputErr);
            });
            return null;
        }));
    }

    /** Test that a transaction must have only one output of type PkgOfferState */
    @Test
    public void transactionMustHaveOnlyOneOutputOfTypePkgOfferState() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updatePkgOutputErr);
            });
            return null;
        }));
    }

    /** Test that the <linearId> parameter of the output state of a transaction must not change */
    @Test
    public void outputStateMustHaveEqualsLinearId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(new UniqueIdentifier(), testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updateLinearIdErr);
            });
            return null;
        }));
    }

    /** Test that the <name> parameter of the output state of a transaction must be well formatted */
    @Test
    public void outputStateMustHaveWellFormattedName() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, null, testDescriptionUpdate,
                            testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(name + strErrMsg);
                });

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, "", testDescriptionUpdate,
                            testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(name + strErrMsg);
                });

                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, " ", testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                return tx.failsWith(name + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <description> parameter of the output state of a transaction must be well formatted */
    @Test
    public void outputStateMustHaveWellFormattedDescription() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, null,
                            testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(description + strErrMsg);
                });

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, "",
                            testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(description + strErrMsg);
                });

                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, " ",
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                return tx.failsWith(description + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <version> parameter of the output state of a transaction must be well formatted */
    @Test
    public void outputStateMustHaveWellFormattedVersion() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                            null, testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(version + strErrMsg);
                });

                tx.tweak(tw -> {
                    tw.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                            "", testPkgInfoId, testLinkUpdate, testPkgType,
                            createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                    return tw.failsWith(version + strErrMsg);
                });

                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        " ", testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                return tx.failsWith(version + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <pkgInfoId> parameter of the output state of a transaction must not change */
    @Test
    public void outputStateMustHaveEqualsIPkgInfoId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, "testPkgInfoId", testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updateInfoIdErr);
            });
            return null;
        }));
    }

    /** Test that the <imageLink> parameter of the output state of a transaction must be a valid url */
    @Test
    public void outputStateMustHaveValidImageLink() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, "httpfg://abc.come", testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(imageLink + strMueErr);
            });
            return null;
        }));
    }

    /** Test that the <pkgType> parameter of the output state of a transaction must not change */
    @Test
    public void outputStateMustHaveEqualsPkgType() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, PkgOfferState.PkgType.PNF,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updatePkgTypeErr);
            });
            return null;
        }));
    }

    /** Test that the <poPrice> parameter of the output state of a transaction must be a valid url */
    @Test
    public void outputStateMustHaveValidPrice() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        null, devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(poPrice + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <author> parameter of the output state of a transaction must not change */
    @Test
    public void outputStateMustHaveEqualsAuthor() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), repositoryNodeTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updateAuthorErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryNode> parameter of the output state of a transaction must not change */
    @Test
    public void outputStateMustHaveEqualsRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateInputPkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), devTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(updateRepositoryNodeErr);
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
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

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
                StateAndRef<PkgOfferState> inputPkg = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeUpdated);
                tx.input(inputPkg.getRef());
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testNameUpdate, testDescriptionUpdate,
                        testVersionUpdate, testPkgInfoId, testLinkUpdate, testPkgType,
                        createProductOfferingPrice(), devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey()),
                        new PkgOfferContract.Commands.UpdatePkg());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}
