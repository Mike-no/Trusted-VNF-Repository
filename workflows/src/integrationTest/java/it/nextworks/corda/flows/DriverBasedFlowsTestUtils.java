package it.nextworks.corda.flows;

public class DriverBasedFlowsTestUtils {
    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=DevTest,L=Pisa,C=IT";
    public static final String buyerX500Name          = "O=BuyerTest,L=Pistoia,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg    = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg        = "it.nextworks.corda.flows";
    public static final String cordAppFinance         = "net.corda.finance.contracts";

    public static final String notaryUsername         = "notaryUsername";
    public static final String notaryPsw              = "notaryPsw";
    public static final String integrationTestEx      = "Caught exception during test: ";
}
