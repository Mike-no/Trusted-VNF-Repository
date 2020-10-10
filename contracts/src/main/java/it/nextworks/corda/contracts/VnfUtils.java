package it.nextworks.corda.contracts;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.Currency;
import java.util.Locale;

/** Simply a class with various utility functions and statements */
public class VnfUtils {

    public static final String createVnfInputErr   = "No input should be consumed when create a VNF";
    public static final String createVnfOutputErr  = "There should be one output state of type VnfState";

    public static final String linearId            = "The <linearId>";
    public static final String name                = "The <name>";
    public static final String description         = "The <description>";
    public static final String serviceType         = "The <serviceType>";
    public static final String version             = "The <version>";
    public static final String requirements        = "The <requirements>";
    public static final String resources           = "The <resources>";
    public static final String imageLink           = "The <imageLink>";
    public static final String repositoryLink      = "The <repositoryLink>";
    public static final String repositoryHashErr   = "The <repositoryHash> parameter does not match the hash value of " +
            "the <repositoryLink> parameter";
    public static final String price               = "The <price>";

    public static final String author              = "The <author>";
    public static final String repositoryNode      = "The <repositoryNode>";

    public static final String strErrMsg           = " parameter cannot be null, empty or only composed by whitespace";
    public static final String strNullErr          = " parameter cannot be null";
    public static final String strMueErr           = " parameter does not represent a valid URL";

    public static final String sameEntityErr       = "The <author> parameter and the <repositoryNode> parameter cannot " +
            "be the same entity";

    public static final String twoSignersErr       = "There must be two signers";
    public static final String mustBeSignersErr    = "<author> and <repositoryNode> must be signers";

    public static final String unknownCommand      = "Unknown command";

    /** Attributes used to construct transactions inside tests */
    public static final UniqueIdentifier testId    = new UniqueIdentifier();

    public static final String testName            = "testVNF";
    public static final String testDescription     = "test";
    public static final String testServiceType     = "testService";
    public static final String testVersion         = "1.0";
    public static final String testRequirements    = "n.b";
    public static final String testResources       = "n.b";
    public static final String testLink            = "https://www.nextworks.it/";
    public static final int testRepositoryHash     = testLink.hashCode();
    public static final Amount<Currency> testPrice = new Amount<>(1,
            Currency.getInstance(Locale.ITALY));

    public static final String devX500Name         = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name  = "O=RepositoryNodeTest,L=Pisa,C=IT";
    public static final String cordAppContractsPkg = "it.nextworks.corda.contracts";
    public static final String cordAppFinancePkg   = "net.corda.finance.contracts";

    public static final String buyVnfInputCashEmp  = "There should be at least one input of type Cash.State";
    public static final String buyVnfInputCashErr  = "There should be only inputs of Cash.State type";
    public static final String buyVnfOutputCashErr = "There should be only outputs of type Cash.State " +
            "and VnfLicenseState";
    public static final String buyVnfOutputCashEmp = "There should be at least one output of type Cash.State";
    public static final String buyVnfLicenseOutErr = "There should be only one output of type VnfLicenseState";

    public static final String buyer               = "The <buyer>";

    public static final String cannotDiffer        = " parameter cannot differ from the specified VnfState entry related";
    public static final String differentAmountErr  = "The output Cash state for the repositoryNode " +
            "differs from the vnf price";
    public static final String buyerSameIdentity   = "The <buyer> parameter and the <repositoryNode> parameter cannot " +
            "be the same entity";
    public static final String mustBeSignersLicErr = "<buyer> and <repositoryNode> must be signers";

    public static final String buyerX500Name       = "O=Buyer,L=Pisa,C=IT";
    public static final String toBeLicensed        = "toBeLicensed";
}
