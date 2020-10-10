package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.VnfLicenseState;
import it.nextworks.corda.states.VnfState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.PartyAndReference;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.dsl.TransactionDSL;
import net.corda.testing.node.MockServices;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Currency;
import java.util.Locale;

import static it.nextworks.corda.contracts.VnfUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

/** Test VnfLicenseContract class in cases where the command used in the transaction is BuyVNF */
public class BuyVnfContractTest {

    /** Simulate a Corda Network composed by three nodes: a buyer, a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity buyerTest =
            new TestIdentity(CordaX500Name.parse(buyerX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(Arrays.asList(cordAppContractsPkg, cordAppFinancePkg), devTest,
                    buyerTest, repositoryNodeTest);

    /**
     * Function used to generate a transaction that will output a VnfState that will
     * be used in a VnfLicenseState transaction as component of a VnfLicenseState
     * @param tx transaction that will output a VnfState
     */
    private void generateVnfState(@NotNull TransactionDSL<?> tx) {
        tx.output(VnfContract.ID, toBeLicensed, new VnfState(testId, testName, testDescription, testServiceType,
                testVersion, testRequirements, testResources, testLink, testLink, testRepositoryHash, testPrice,
                devTest.getParty(), repositoryNodeTest.getParty()));
        tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                new VnfContract.Commands.CreateVNF());
    }

    /**
     * Generate a Cash state with default opaque bytes
     * @param owner  the Party that will own this Cash state
     * @param amount the amount of the Cash state
     * @return       a brand new Cash.State object
     */
    private Cash.State createCashState(AbstractParty owner, Amount<Currency> amount) {
        OpaqueBytes defaultBytes = new OpaqueBytes(new byte[1]);
        PartyAndReference partyAndReference = new PartyAndReference(owner, defaultBytes);
        return new Cash.State(partyAndReference, amount, owner);
    }

    /** Test that a transaction must include the BuyVNF command */
    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.fails();
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.verifies();
            });
            return null;
        }));
    }

    /** Test that the transaction must have only one VnfLicenseState output state */
    @Test
    public void transactionMustHaveOnlyOneVnfLicenseStateOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(buyVnfLicenseOutErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryLink> parameter is equal to the <repositoryLink> parameter of the VnfState */
    @Test
    public void transactionMustHaveCorrectRepositoryLink() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, "",
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(repositoryLink + cannotDiffer);
            });
            return null;
        }));
    }

    /** Test that the <repositoryHash> parameter is equal to the <repositoryHash> parameter of the VnfState */
    @Test
    public void transactionMustHaveCorrectRepositoryHash() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        0, buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(repositoryHashErr);
            });
            return null;
        }));
    }

    /** Test that the <price> parameter is equal to the <price> parameter of the VnfState */
    @Test
    public void transactionMustHaveCorrectCashAmount() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(2,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(differentAmountErr);
            });
            return null;
        }));
    }

    /** Test that the <buyer> parameter of the VnfLicenceState must not be null */
    @Test
    public void transactionMustHaveValidBuyer() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), null, vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(buyer + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <repositoryNode> parameter is equal to the <repositoryNode> parameter of the VnfState */
    @Test
    public void transactionMustHaveCorrectRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(devTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), devTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), devTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(repositoryNode + cannotDiffer);
            });
            return null;
        }));
    }

    /** Test that the <buyer> parameter must not be equal the <repositoryNode> parameter */
    @Test
    public void buyerIsNotRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), repositoryNodeTest.getParty(), repositoryNodeTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(buyerSameIdentity);
            });
            return null;
        }));
    }

    /** Test that the <buyer> must sign the transaction */
    @Test
    public void buyerMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

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
                generateVnfState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<VnfState> vnfLicensed = ledger.retrieveOutputStateAndRef(VnfState.class, toBeLicensed);
                VnfState vnf = vnfLicensed.getState().getData();
                tx.output(VnfLicenseContract.ID, new VnfLicenseState(vnfLicensed, vnf.getRepositoryLink(),
                        vnf.getRepositoryHash(), buyerTest.getParty(), vnf.getRepositoryNode()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey()),
                        new VnfLicenseContract.Commands.BuyVNF());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}
