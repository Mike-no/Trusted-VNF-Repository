package it.nextworks.corda.contracts;

/** Simply a class with various utility functions and statements */
public class VnfUtils {

    public static final String createVnfInputErr  = "No input should be consumed when create a VNF";
    public static final String createVnfOutputErr = "There should be one output state of type VnfState";

    public static final String linearId           = "The <linearId>";
    public static final String name               = "The <name>";
    public static final String description        = "The <description>";
    public static final String serviceType        = "The <serviceType>";
    public static final String version            = "The <version>";
    public static final String requirements       = "The <requirements>";
    public static final String resources          = "The <resources>";
    public static final String imageLink          = "The <imageLink>";
    public static final String repositoryLink     = "The <repositoryLink>";
    public static final String repositoryHashErr  = "The <repositoryHash> parameter does not match the hash value of " +
            "the <repositoryLink> parameter";
    public static final String price              = "The <price>";

    public static final String author             = "The <author>";
    public static final String repositoryNode     = "The <repositoryNode>";

    public static final String strErrMsg          = " parameter cannot be null, empty or only composed by whitespace";
    public static final String strNullErr         = " parameter cannot be null";
    public static final String strMueErr          = " parameter does not represent a valid URL";

    public static final String sameEntityErr      = "The <author> parameter and the <repositoryNode> parameter cannot " +
            "be the same entity";

    public static final String twoSignersErr      = "There must be two signers";
    public static final String mustBeSignersErr   = "<author> and <repositoryNode> must be signers";

    public static final String unknownCommand     = "Unknown command";
}
