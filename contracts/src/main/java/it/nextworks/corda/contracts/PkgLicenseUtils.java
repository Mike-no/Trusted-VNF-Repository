package it.nextworks.corda.contracts;

public class PkgLicenseUtils {
    public static final String buyPkgInputCashEmp = "There should be at least one input of type Cash.State";
    public static final String buyPkgInputCashErr = "There should be only inputs of Cash.State type.";
    public static final String buyPkgOutputCashErr = "There should be only outputs of type Cash.State " +
            "and PkgLicenseState";
    public static final String buyPkgOutputCashEmp = "There should be at least one output of type Cash.State";
    public static final String buyPkgLicenseOutErr = "There should be only one output of type PkgLicenseState";

    public static final String buyer               = "The <buyer>";

    public static final String strNullErr          = " parameter cannot be null.";
    public static final String differentAmountErr  = "The output Cash state for the repositoryNode " +
            "differs from the package price";

    public static final String buyerSameIdentity   = "The <buyer> parameter and the <repositoryNode> parameter cannot " +
            "be the same entity";
    public static final String buyerAndAuthorSame  = "The <buyer> parameter and the <author> parameter cannot " +
            "be the same entity";

    public static final String mustBeSignersLicErr = "<buyer> and <repositoryNode> must be signers";
    public static final String twoSignersErr       = "There must be two signers.";

    public static final String unknownCommand      = "Unknown command.";

    /** Attributes used to construct transactions inside tests */
    public static final String buyerX500Name       = "O=BuyerTest,L=Pisa,C=IT";
    public static final String devX500Name         = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name  = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppFinancePkg   = "net.corda.finance.contracts";
    public static final String cordAppContractsPkg = "it.nextworks.corda.contracts";

    public static final String toBeLicensed        = "toBeLicensed";
}
