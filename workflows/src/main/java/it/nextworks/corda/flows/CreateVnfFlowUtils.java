package it.nextworks.corda.flows;

import net.corda.core.contracts.Amount;

import java.util.Currency;
import java.util.Locale;

public class CreateVnfFlowUtils {
    public static final String GENERATING_TRANSACTION = "Generating transaction based on new VNF.";
    public static final String VERIFYING_TRANSACTION  = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS        = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String testName               = "testVNF";
    public static final String testDescription        = "test";
    public static final String testServiceType        = "testService";
    public static final String testVersion            = "1.0";
    public static final String testRequirements       = "n.b";
    public static final String testResources          = "n.b";
    public static final String testLink               = "https://www.nextworks.it/";
    public static final Amount<Currency> testPrice    = new Amount<>(1,
            Currency.getInstance(Locale.ITALY));

    public static final String notVnfStateErr         = "This must be a VNF transaction.";

    public static final String cordAppContractsPkg     = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg         = "it.nextworks.corda.flows";

    public static final String notaryUsername          = "notaryUsername";
    public static final String notaryPsw               = "notaryPsw";
    public static final String integrationTestEx       = "Caught exception during test: ";
}
