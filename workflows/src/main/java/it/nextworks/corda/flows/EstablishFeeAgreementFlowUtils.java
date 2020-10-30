package it.nextworks.corda.flows;

public class EstablishFeeAgreementFlowUtils {
    public static final String feeErr                     = "The specified fee must be between 0 and 100 : [0-100]";
    public static final String notFeeAgreementErr         = "This must be a fee transaction.";
    public static final String tooHighFee                 = "The fee requested by the Repository is 10%";

    public static final String AlreadyEstablishedFee      = "The developer has already established a fee agreement" +
            " with the Repository Node.";

    public static final String VERIFY_AGREEMENT_EXISTENCE = "Verify the existence of previous agreement.";
    public static final String GENERATING_TRANSACTION     = "Generating transaction based on new fee establishing request.";
    public static final String VERIFYING_TRANSACTION      = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION        = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS            = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION     = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name             = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name                = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name         = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg        = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg            = "it.nextworks.corda.flows";
}
