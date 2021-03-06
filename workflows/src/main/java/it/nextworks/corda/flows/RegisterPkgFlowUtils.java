package it.nextworks.corda.flows;

public class RegisterPkgFlowUtils {
    public static final String GENERATING_TRANSACTION = "Generating transaction based on new package.";
    public static final String VERIFYING_TRANSACTION  = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS        = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String notPkgStateErr         = "This must be a package transaction.";
    public static final String malformedDbTable       = "The Database table cannot be used: malformed column(s).";
    public static final String notExistingAgreement   = "The developer hasn't already establish a fee agreement " +
            "with the Repository Node.";

    public static final String cordAppContractsPkg    = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg        = "it.nextworks.corda.flows";
}
