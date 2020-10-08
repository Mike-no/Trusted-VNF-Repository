package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;

import static it.nextworks.corda.contracts.VnfUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

/** Test VnfContract class in cases where the command used in the transaction is CreateVNF */
public class CreateVnfContractTest {

    /** Simulate a Corda Network composed by two nodes: a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(Arrays.asList(cordAppContractsPkg),
                    devTest, repositoryNodeTest);

    /** Attributes used to construct transactions inside tests */
    private static final UniqueIdentifier id    = new UniqueIdentifier();

    private static final String name            = "testVNF";
    private static final String description     = "test";
    private static final String serviceType     = "testService";
    private static final String version         = "1.0";
    private static final String requirements    = "n.b";
    private static final String resources       = "n.b";
    private static final String link            = "https://www.nextworks.it/";
    private static final int repositoryHash     = link.hashCode();
    private static final Amount<Currency> price = new Amount<>(1,
                    Currency.getInstance(Locale.ITALY));

    /** Test that a transaction must include the CreateVNF command */
    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(VnfContract.ID, new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty()));
                tx.fails();
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());
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
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.input(VnfContract.ID, vnfState);
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(VnfUtils.createVnfInputErr);
            });
            return null;
        }));
    }

    /** Test that a transaction must have one output */
    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(VnfUtils.createVnfOutputErr);
            });
            return null;
        }));
    }

    /** Test that the <linearId> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidId() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(null, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(linearId + strNullErr);
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
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, null, description, serviceType, version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.name + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, "", description, serviceType, version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.name + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, " ", description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.name + strErrMsg);
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
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, null, serviceType, version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.description + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, "", serviceType, version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.description + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, name, " ", serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.description + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <serviceType> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedServiceType() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, null, version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.serviceType + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, "", version,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.serviceType + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, name, description, " ", version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.serviceType + strErrMsg);
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
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, null,
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.version + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, "",
                            requirements, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.version + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, name, description, serviceType, " ",
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.version + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <requirements> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedRequirements() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                            null, resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.requirements + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                            "", resources, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.requirements + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        " ", resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.requirements + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <requirements> parameter of the output state of a transaction must be well formatted */
    @Test
    public void stateMustHaveWellFormattedResources() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                            requirements, null, link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.resources + strErrMsg);
                });

                tx.tweak(tw -> {
                    VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                            requirements, "", link, link, repositoryHash, price,
                            devTest.getParty(), repositoryNodeTest.getParty());
                    tw.output(VnfContract.ID, vnfState);

                    return tw.failsWith(VnfUtils.resources + strErrMsg);
                });

                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, " ", link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);

                return tx.failsWith(VnfUtils.resources + strErrMsg);
            });
            return null;
        }));
    }

    /** Test that the <imageLink> parameter of the output state of a transaction must be a valid url */
    @Test
    public void stateMustHaveValidImageLink() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, "httpfg://abc.come", link, repositoryHash,
                        price, devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(imageLink + strMueErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryLink> parameter of the output state of a transaction must be a valid url */
    @Test
    public void stateMustHaveValidRepositoryLink() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, "httpfg://abc.come", repositoryHash,
                        price, devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(repositoryLink + strMueErr);
            });
            return null;
        }));
    }

    /**
     * Test that the <repositoryHash> parameter of the output state of a transaction must be
     * equals the hash of the <repositoryLink> hash
     */
    @Test
    public void stateMustHaveValidRepositoryHash() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, 123,
                        price, devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(repositoryHashErr);
            });
            return null;
        }));
    }

    /** Test that the <author> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidAuthor() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        null, repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

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
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), null);
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

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
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), devTestDupe.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

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
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

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
                VnfState vnfState = new VnfState(id, name, description, serviceType, version,
                        requirements, resources, link, link, repositoryHash, price,
                        devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(VnfContract.ID, vnfState);
                tx.command(ImmutableList.of(devTest.getPublicKey()),
                        new VnfContract.Commands.CreateVNF());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}