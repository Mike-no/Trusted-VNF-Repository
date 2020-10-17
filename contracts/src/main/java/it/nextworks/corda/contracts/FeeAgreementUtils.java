package it.nextworks.corda.contracts;

public class FeeAgreementUtils {
    public static final String createAgreementInputErr  = "No input should be consumed when create an agreement.";
    public static final String createAgreementOutputErr = "There should be one output state of type FeeAgreementState";

    public static final String nullDeveloper            = "The <developer> parameter cannot be null.";
    public static final String nullRepositoryNode       = "The <repositoryNode> parameter cannot be null.";
    public static final String sameEntityErr            = "The <developer> parameter and the <repositoryNode> " +
            "parameter cannot be the same entity.";

    public static final String twoSignersErr            = "There must be two signers.";
    public static final String mustBeSignersErr         = "<developer> and <repositoryNode> must be signers.";

    public static final String unknownCommand           = "Unknown command.";

    /** Attributes used to construct transactions inside tests */
    public static final String devX500Name         = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name  = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg = "it.nextworks.corda.contracts";
}
