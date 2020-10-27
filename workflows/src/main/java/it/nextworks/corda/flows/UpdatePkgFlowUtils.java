package it.nextworks.corda.flows;

public class UpdatePkgFlowUtils {
    public static final String nullHttpRequest         = "The <httpRequest> parameter cannot be null.";

    public static final String RETRIEVING_PKG_FROM_LID = "Retrieving the package offer state to update from the vault.";
    public static final String SENDING_PATH_REQUEST    = "Sending the base path of the http request.";
    public static final String GENERATING_TRANSACTION  = "Generating transaction based on new package.";
    public static final String VERIFYING_TRANSACTION   = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION     = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS         = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION  = "Obtaining Notary signature and recording transaction.";

    public static final String AWAITING_PATH_REQUEST   = "Waiting for the base path of the http request.";
    public static final String VERIFYING_RCV_DATA      = "Verifying the information received.";

    public static final String notBasePathErr          = "The base path for the http request is malformed.";
    public static final String nonExistentPkg          = "Cannot find package: ";
    public static final String notPkgStateErr          = "This must be a package update transaction.";
    public static final String pkgMustBeRemovedFirst   = "The package must be removed from the 5g-catalogue before update.";

    public static final String notaryX500Name          = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name             = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name      = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg     = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg         = "it.nextworks.corda.flows";
}
