package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.PkgLicenseState;
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

import static it.nextworks.corda.contracts.PkgLicenseUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

/** Test PkgLicenseContract class in cases where the command used in the transaction is BuyPkg */
public class BuyPkgOfferContractTest {

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
     * Function used to generate a transaction that will output a PkgOfferState that will
     * be used in a PkgLicenseState transaction as component of a PkgLicenseState
     * @param tx transaction that will output a PkgOfferState
     */
    private void generatePkgOfferState(@NotNull TransactionDSL<?> tx) {
        tx.output(PkgOfferContract.ID, toBeLicensed, new PkgOfferState(PkgOfferUtils.testId, PkgOfferUtils.testName,
                PkgOfferUtils.testDescription, PkgOfferUtils.testVersion, PkgOfferUtils.testPkgInfoId,
                PkgOfferUtils.testLink, PkgOfferUtils.testPrice, PkgOfferUtils.testPkgType, devTest.getParty(),
                repositoryNodeTest.getParty()));
        tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                new PkgOfferContract.Commands.RegisterPkg());
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

    /** Test that a transaction must include the BuyPkg command */
    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.fails();
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.verifies();
            });
            return null;
        }));
    }

    /** Test that the transaction must have only one PkgLicenseState output state */
    @Test
    public void transactionMustHaveOnlyOnePkgLicenseStateOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(buyPkgLicenseOutErr);
            });
            return null;
        }));
    }

    /** Test that the <price> parameter is equal to the <price> parameter of the PkgOfferState */
    @Test
    public void transactionMustHaveCorrectCashAmount() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(2,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(differentAmountErr);
            });
            return null;
        }));
    }

    /** Test that the <buyer> parameter of the PkgLicenceState must not be null */
    @Test
    public void transactionMustHaveValidBuyer() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, null));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(buyer + strNullErr);
            });
            return null;
        }));
    }

    /** Test that the <buyer> parameter must not be equal the <repositoryNode> parameter */
    @Test
    public void buyerIsNotRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, repositoryNodeTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(buyerSameIdentity);
            });
            return null;
        }));
    }

    /** Test that the <buyer> parameter must not be equal the <author> parameter */
    @Test
    public void buyerIsNotAuthor() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(devTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, devTest.getParty()));

                tx.command(devTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(buyerAndAuthorSame);
            });
            return null;
        }));
    }

    /** Test that the <buyer> must sign the transaction */
    @Test
    public void buyerMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

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
                generatePkgOfferState(tx);
                return tx.verifies();
            });

            ledger.transaction(tx -> {
                Cash.State inputCash = createCashState(buyerTest.getParty(), new Amount<>(1,
                        Currency.getInstance(Locale.ITALY)));
                tx.input(Cash.class.getName(), inputCash);

                tx.output(Cash.class.getName(), inputCash.withNewOwner(repositoryNodeTest.getParty()).getOwnableState());

                StateAndRef<PkgOfferState> pkgLicensed = ledger.retrieveOutputStateAndRef(PkgOfferState.class, toBeLicensed);
                tx.output(PkgLicenseContract.ID, new PkgLicenseState(pkgLicensed, buyerTest.getParty()));

                tx.command(buyerTest.getPublicKey(), new Cash.Commands.Move());
                tx.command(ImmutableList.of(buyerTest.getPublicKey()),
                        new PkgLicenseContract.Commands.BuyPkg());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}
