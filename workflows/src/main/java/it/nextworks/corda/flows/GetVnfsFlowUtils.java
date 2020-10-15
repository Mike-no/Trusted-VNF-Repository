package it.nextworks.corda.flows;

public class GetVnfsFlowUtils {

    public static final String nullContainerErr     = "The received container is null.";
    public static final String nullEntryInContainer = "The received container contains null VNF info entry.";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=DevTest,L=Pisa,C=IT";
    public static final String buyerX500Name          = "O=BuyerTest,L=Pistoia,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg    = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg        = "it.nextworks.corda.flows";
}
