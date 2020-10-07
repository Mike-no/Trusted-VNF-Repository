package it.nextworks.corda.flows;

public class CreateVnfFlowUtils {
    public static final String GENERATING_TRANSACTION = "Generating transaction based on new VNF.";
    public static final String VERIFYING_TRANSACTION  = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS        = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=Dev,L=Pisa,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String notVnfStateErr         = "This must be a VNF transaction.";
}
