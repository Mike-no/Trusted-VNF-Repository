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

import static net.corda.testing.node.NodeTestUtils.ledger;


public class CreateVnfContractTest {

    private static final TestIdentity devTest =
            new TestIdentity(new CordaX500Name("devTest",
                    "Pisa", "IT"));
    private static final TestIdentity repositoryNodeTest =
            new TestIdentity(new CordaX500Name("nextworks",
                    "Pisa", "IT"));
    private static final MockServices ledgerServices =
            new MockServices(Arrays.asList("it.nextworks.corda.contracts"),
                    devTest, repositoryNodeTest);

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


}