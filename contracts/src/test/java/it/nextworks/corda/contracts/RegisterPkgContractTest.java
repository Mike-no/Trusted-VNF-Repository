package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static it.nextworks.corda.contracts.PkgOfferUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

/** Test PkgOfferContract class in cases where the command used in the transaction is RegisterPkg */
public class RegisterPkgContractTest {

    /** Simulate a Corda Network composed by two nodes: a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(ImmutableList.of(cordAppContractsPkg),
                    devTest, repositoryNodeTest);

    /** Test that a transaction must include the RegisterPkg command */
    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(PkgOfferContract.ID, new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty()));
                tx.fails();
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.verifies();
            });
            return null;
        }));
    }

    /**
     * Test that a transaction must have no input (does not consume input)
     * N.B the log of this test will contain an unexpected exception log as we can
     * read here https://github.com/corda/corda/issues/6050
     */
    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.input(PkgOfferContract.ID, pkgOfferState);
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(PkgOfferUtils.createPkgInputErr);
            });
            return null;
        }));
    }

    /** Test that a transaction must have one output */
    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(PkgOfferUtils.createPkgOutputErr);
            });
            return null;
        }));
    }

    /** Test that the <name> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedName() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, null, testDescription, testVersion,
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.name + strErrMsg);
                });

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, "", testDescription, testVersion,
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.name + strErrMsg);
                });

                PkgOfferState pkgOfferState = new PkgOfferState(testId, " ", testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);

                return tx.failsWith(PkgOfferUtils.name + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <description> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedDescription() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, null, testVersion,
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.description + strErrMsg);
                });

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, "", testVersion,
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.description + strErrMsg);
                });

                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, " ", testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);

                return tx.failsWith(PkgOfferUtils.description + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <version> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedVersion() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, null,
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.version + strErrMsg);
                });

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, "",
                            testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(PkgOfferUtils.version + strErrMsg);
                });

                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, " ",
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);

                return tx.failsWith(PkgOfferUtils.version + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <repositoryLink> parameter of the output state of a transaction must be a valid url */
    @Test
    public void stateMustHaveValidPkgInfoId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                            null, testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(pkgInfoId + strErrMsg);
                });

                tx.tweak(tw -> {
                    PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                            "", testLink, testPrice, testPkgType, devTest.getParty(),
                            repositoryNodeTest.getParty());
                    tw.output(PkgOfferContract.ID, pkgOfferState);

                    return tw.failsWith(pkgInfoId + strErrMsg);
                });

                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        " ", testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);

                return tx.failsWith(pkgInfoId + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <imageLink> parameter of the output state of a transaction must be a valid url */
    @Test
    public void stateMustHaveValidImageLink() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, "httpfg://abc.come", testPrice, testPkgType,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(imageLink + strMueErr);
            });
            return null;
        }));
    }

    /** Test that the <price> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidPrice() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, null, testPkgType,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(price + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <pkgType> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidPkgType() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, null,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(pkgTypeErr);
            });
            return null;
        }));
    }

    /** Test that the <author> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidAuthor() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, null,
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(author + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryNode> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        null);
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(repositoryNode + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <author> parameter must not be equal the <repositoryNode> parameter */
    @Test
    public void devIsNotRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            final TestIdentity devTestDupe = new TestIdentity(devTest.getName(), devTest.getKeyPair());
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        devTestDupe.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(sameEntityErr);
            });
            return null;
        }));
    }

    /** Test that the <author> must sign the transaction */
    @Test
    public void authorMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

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
                PkgOfferState pkgOfferState = new PkgOfferState(testId, testName, testDescription, testVersion,
                        testPkgInfoId, testLink, testPrice, testPkgType, devTest.getParty(),
                        repositoryNodeTest.getParty());
                tx.output(PkgOfferContract.ID, pkgOfferState);
                tx.command(ImmutableList.of(devTest.getPublicKey()),
                        new PkgOfferContract.Commands.RegisterPkg());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}