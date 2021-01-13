package it.nextworks.corda.webserver;

public class ControllerUtils {
    public static final String nullParam                  = "cannot accept null parameter.";

    public static final String statusRequestOK            = "Status retrieve request processed.";
    public static final String serverTimeRequestOK        = "Server Time retrieve request processed.";
    public static final String addressesRequestOK         = "Addresses retrieve request processed.";
    public static final String identitiesRequestOK        = "Identities retrieve request processed.";
    public static final String platformVersionRequestOK   = "Platform Version retrieve request processed.";
    public static final String notariesRequestOK          = "Notaries retrieve request processed.";
    public static final String flowsRequestOK             = "Flows retrieve request processed.";
    public static final String meRequestOK                = "Me retrieve request processed.";
    public static final String peersRequestOK             = "Peers retrieve request processed.";

    public static final String feeAgreementEstablished    = "Fee Agreement established with a fee of 10%.";
    public static final String feeAgreementFailed         = "Fee Agreement failed : ";

    public static final String pkgRegisterFailed          = "Package register failed : ";
    public static final String onBoardingRequired         = "You must onboard the package first.";
    public static final String errorWhileProcessingRq     = "Error while processing request.";
    public static final String getRequestSucceed          = "GET request to 5g-catalogue retrieve the requested package.";
    public static final String pkgRegistered              = "Registered package: ";

    public static final String pkgUpdated                 = "Updated package: ";
    public static final String pkgUpdateFailed            = "Package update failed : ";
    public static final String notExistingPkg             = "The specified package to update does not exist.";

    public static final String marketplaceRequestOK       = "Marketplace retrieve request processed.";
    public static final String pkgsGetFailed              = "Packages retrieve Failed : ";
    public static final String badRequestValue            = "The requested value criteria cannot be processed.";

    public static final String cashIssued                 = "Required cash amount issued.";
    public static final String cashIssueFailed            = "Cash issue Failed : ";
    public static final String negativeAmount             = "The <amount> parameter cannot be negative.";
    public static final String invalidISOCode             = "The <currency> parameter is not a valid ISO 4217 code.";

    public static final String pkgPurchased               = "Purchased package: ";
    public static final String pkgPurchaseFailed          = "Package purchase Failed : ";

    public static final String pkgDeleted                 = "Deleted package: ";
    public static final String pkgDeleteFailed            = "Package delete Failed : ";

    public static final String cashBalancesRequestOK      = "Cash Balances retrieve request processed.";
    public static final String feeAgreementStateRequestOK = "Fee Agreement State retrieve request processed.";
    public static final String pkgOfferStateRequestOK     = "Package Offer State retrieve request processed.";
    public static final String pkgLicenseStateRequestOK   = "Package License State retrieve request processed.";
}
