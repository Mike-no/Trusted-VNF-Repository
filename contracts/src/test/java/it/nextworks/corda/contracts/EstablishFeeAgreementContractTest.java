package it.nextworks.corda.contracts;

import com.google.common.collect.ImmutableList;
import it.nextworks.corda.states.FeeAgreementState;
import net.corda.core.identity.CordaX500Name;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockServices;
import org.junit.Test;

import static it.nextworks.corda.contracts.FeeAgreementUtils.*;
import static net.corda.testing.node.NodeTestUtils.ledger;

public class EstablishFeeAgreementContractTest {

    /** Simulate a Corda Network composed by two nodes: a developer and the repositoryNode */
    private static final TestIdentity devTest =
            new TestIdentity(CordaX500Name.parse(devX500Name));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(CordaX500Name.parse(repositoryX500Name));
    private static final MockServices ledgerServices =
            new MockServices(ImmutableList.of(cordAppContractsPkg),
                    devTest, repositoryNodeTest);

    /** Test that a transaction must include the EstablishFeeAgreement command */
    @Test
    public void transactionMustIncludeCreateCommand() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, devTest.getParty(), repositoryNodeTest.getParty()));
                tx.fails();
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.verifies();
            });
            return null;
        }));
    }

    /** Test that a transaction must have no input (does not consume input) */
    @Test
    public void transactionMustHaveNoInputs() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                FeeAgreementState feeAgreementState =
                        new FeeAgreementState(10, devTest.getParty(), repositoryNodeTest.getParty());
                tx.input(FeeAgreementContract.ID, feeAgreementState);
                tx.output(FeeAgreementContract.ID, feeAgreementState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(createAgreementInputErr);
            });
            return null;
        }));
    }

    /** Test that a transaction must have one output */
    @Test
    public void transactionMustHaveOneOutput() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                FeeAgreementState feeAgreementState =
                        new FeeAgreementState(10, devTest.getParty(), repositoryNodeTest.getParty());
                tx.output(FeeAgreementContract.ID, feeAgreementState);
                tx.output(FeeAgreementContract.ID, feeAgreementState);
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(createAgreementOutputErr);
            });
            return null;
        }));
    }

    /** Test that the <developer> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidDeveloper() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, null, repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(nullDeveloper);
            });
            return null;
        }));
    }

    /** Test that the <repositoryNode> parameter of the output state of a transaction must not be null */
    @Test
    public void stateMustHaveValidRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, devTest.getParty(), null));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(nullRepositoryNode);
            });
            return null;
        }));
    }

    /** Test that the <developer> parameter must not be equal the <repositoryNode> parameter */
    @Test
    public void devIsNotRepositoryNode() {
        ledger(ledgerServices, (ledger -> {
            final TestIdentity devTestDupe = new TestIdentity(devTest.getName(), devTest.getKeyPair());
            ledger.transaction(tx -> {
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, devTest.getParty(), devTestDupe.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey(), repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(sameEntityErr);
            });
            return null;
        }));
    }

    /** Test that the <developer> must sign the transaction */
    @Test
    public void authorMustSignTransaction() {
        ledger(ledgerServices, (ledger -> {
            ledger.transaction(tx -> {
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(repositoryNodeTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

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
                tx.output(FeeAgreementContract.ID,
                        new FeeAgreementState(10, devTest.getParty(), repositoryNodeTest.getParty()));
                tx.command(ImmutableList.of(devTest.getPublicKey()),
                        new FeeAgreementContract.Commands.EstablishFeeAgreement());

                return tx.failsWith(twoSignersErr);
            });
            return null;
        }));
    }
}
