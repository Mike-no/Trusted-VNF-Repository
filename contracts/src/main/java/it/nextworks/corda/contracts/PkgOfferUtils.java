package it.nextworks.corda.contracts;

import it.nextworks.corda.states.PkgOfferState;
import it.nextworks.corda.states.productOfferingPrice.Money;
import it.nextworks.corda.states.productOfferingPrice.Quantity;
import it.nextworks.corda.states.productOfferingPrice.TimePeriod;
import net.corda.core.contracts.UniqueIdentifier;

/** Simply a class with various utility functions and statements */
public class PkgOfferUtils {

    /* Utils for the RegisterPkg Command */

    public static final String createPkgInputErr   = "No input should be consumed when create a package.";
    public static final String createPkgOutputErr  = "There should be one output state of type PkgOfferState.";

    public static final String name                = "The <name>";
    public static final String description         = "The <description>";
    public static final String version             = "The <version>";
    public static final String imageLink           = "The <imageLink>";
    public static final String pkgInfoId           = "The <pkgInfoId>";
    public static final String poPrice             = "The <poPrice>";

    public static final String author              = "The <author>";
    public static final String repositoryNode      = "The <repositoryNode>";

    public static final String strErrMsg           = " parameter cannot be null, empty or only composed by whitespace.";
    public static final String strNullErr          = " parameter cannot be null.";
    public static final String strMueErr           = " parameter does not represent a valid URL.";
    public static final String pkgTypeErr          = "The <pkgType> parameter must be VNF or PNF.";

    public static final String sameEntityErr       = "The <author> parameter and the <repositoryNode> parameter cannot " +
            "be the same entity.";

    public static final String twoSignersErr       = "There must be two signers.";
    public static final String mustBeSignersErr    = "<author> and <repositoryNode> must be signers.";

    public static final String unknownCommand      = "Unknown command.";

    /** Attributes used to construct transactions inside tests */

    public static final String devX500Name                = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name         = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg        = "it.nextworks.corda.contracts";

    public static final UniqueIdentifier testId           = new UniqueIdentifier();

    public static final String testName                   = "testVNF";
    public static final String testDescription            = "test";
    public static final String testVersion                = "1.0";
    public static final String testPkgInfoId              = "28a4fbc4-3cca-424f-b15a-0c6a5c4b49ab";
    public static final String testLink                   = "https://www.nextworks.it/";
    public static final PkgOfferState.PkgType testPkgType = PkgOfferState.PkgType.VNF;

    public static final String testPoId                   = "123456789";
    public static final boolean testIsBundle              = false;
    public static final String testLastUpdate             = "2017-08-27T00:00:00.0Z";
    public static final String testLifecycleStatus        = "testLifecycleStatus";
    public static final String testPoName                 = "testPo";
    public static final float testPercentage              = 20;
    public static final String testPriceType              = "testPriceType";
    public static final int testRecChargePeriodLength     = 1;
    public static final String testRecChargePeriodType    = "testChargePeriodType";
    public static final Money testPrice                   = new Money("EUR", 1);
    public static final Quantity testQuantity             = new Quantity(1, "test");
    public static final TimePeriod testValidFor           = new TimePeriod("2020-10-23T16:42:23.0Z",
            "2020-10-24T00:00:00.0Z");

    /* Utils for the UpdatePkg Command */

    public static final String updatePkgInputErr       = "There should be only one input.";
    public static final String updatePkgInputTypeErr   = "There should be only one input of type PkgOfferState.";
    public static final String updatePkgOutputErr      = "There should be only one output.";
    public static final String updatePkgOutputTypeErr  = "There should be only one output of type PkgOfferState.";

    public static final String updateLinearIdErr       = "The <linearId> parameter must not change.";
    public static final String updateInfoIdErr         = "The <pkgInfoId> parameter must not change.";
    public static final String updatePkgTypeErr        = "The <pkgType> parameter must not change.";

    public static final String updateAuthorErr         = "Only the author can update a package and / or the <author> " +
            "parameter cannot change.";
    public static final String updateRepositoryNodeErr = "The <repositoryNode> parameter must not change.";

    public static final String testNameUpdate        = "testVNFUpdate";
    public static final String testDescriptionUpdate = "testUpdate";
    public static final String testVersionUpdate     = "2.0";
    public static final String testLinkUpdate        = "https://www.youtube.com/watch?v=ejYttnAXfEY&t";

    public static final String toBeUpdated           = "toBeUpdated";

    /* Utils for the UpdatePkg Command */

    public static final String deletePkgInputErr     = "There should be only one input.";
    public static final String deletePkgInputTypeErr = "There should be only one input of type PkgOfferState.";
    public static final String deletePkgOutputErr    = "There should not be output.";

    public static final String toBeDeleted           = "toBeDeleted";
}
