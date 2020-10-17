package it.nextworks.corda.contracts;

import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;

import java.util.Currency;
import java.util.Locale;

/** Simply a class with various utility functions and statements */
public class PkgOfferUtils {

    public static final String createPkgInputErr  = "No input should be consumed when create a package.";
    public static final String createPkgOutputErr = "There should be one output state of type PkgOfferState.";

    public static final String name                = "The <name>";
    public static final String description         = "The <description>";
    public static final String version             = "The <version>";
    public static final String imageLink           = "The <imageLink>";
    public static final String pkgInfoId           = "The <pkgInfoId>";
    public static final String price               = "The <price>";

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
    public static final String devX500Name         = "O=DevTest,L=Pisa,C=IT";
    public static final String repositoryX500Name  = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg = "it.nextworks.corda.contracts";

    public static final UniqueIdentifier testId           = new UniqueIdentifier();

    public static final String testName                   = "testVNF";
    public static final String testDescription            = "test";
    public static final String testVersion                = "1.0";
    public static final String testPkgInfoId              = "123";
    public static final String testLink                   = "https://www.nextworks.it/";
    public static final Amount<Currency> testPrice        = new Amount<>(1,
            Currency.getInstance(Locale.ITALY));
    public static final PkgOfferState.PkgType testPkgType = PkgOfferState.PkgType.VNF;
}
