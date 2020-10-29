package it.nextworks.corda.flows;

public class DeletePkgFlowUtils {
    public static final String nullLinearId            = "The <linearId> parameter cannot be null.";
    public static final String nonExistentPkg          = "Cannot find package: ";

    public static final String RETRIEVING_PKG_FROM_LID = "Retrieving the package offer state to update from the vault.";
    public static final String SENDING_PKG_ID          = "Sending the ID of the required package.";
    public static final String GENERATING_TRANSACTION  = "Generating transaction based on the package information.";
    public static final String VERIFYING_TRANSACTION   = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION     = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS         = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION  = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name          = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name             = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name      = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg     = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg         = "it.nextworks.corda.flows";
}
