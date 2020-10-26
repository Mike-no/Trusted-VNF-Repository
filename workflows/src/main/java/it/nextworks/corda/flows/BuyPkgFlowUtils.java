package it.nextworks.corda.flows;

public class BuyPkgFlowUtils {
    public static final String nullPkgId              = "The <PkgId> parameter cannot be null.";
    public static final String nullPrice              = "The <price> parameter cannot be null.";

    public static final String receivedTooMuchStates  = "Received more than one PkgOfferState: " +
            "require only the requested PkgOfferState";
    public static final String requestedPkgErr        = "The received package differ from the requested one.";
    public static final String priceMismatch          = "The received package price differ from the displayed one";
    public static final String repositoryNodeMismatch = "The received package specify a different Repository Node.";
    public static final String missingCash            = "The buyer does not have enough cash to pay the package requested.";
    public static final String nonExistentPkg         = "Cannot find package: ";
    public static final String notBasePathErr         = "The base path for the http request is malformed.";
    public static final String unexpectedInvalidPrice = "The received amount for the payment does not match the package price";

    public static final String SENDING_PKG_ID         = "Sending the ID of the required package.";
    public static final String SENDING_PATH_REQUEST   = "Sending the base path of the http request.";
    public static final String RECEIVING_PKG_INFO     = "Receiving information about the package to buy.";
    public static final String VERIFYING_PKG_INFO     = "Verifying the information received about the package.";
    public static final String GENERATING_TRANSACTION = "Generating transaction based on the package information.";
    public static final String VERIFYING_TRANSACTION  = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS        = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String AWAITING_PKG_ID        = "Waiting for the ID of the package that the buyer want to purchase.";
    public static final String VERIFYING_RCV_DATA     = "Verifying the information received.";
    public static final String AWAITING_PATH_REQUEST  = "Waiting for the base path of the http request.";
    public static final String SENDING_PKG_INFO       = "Sending information about the required package.";
    public static final String SENDING_CASH_TO_AUTHOR = "Sending the amount that belongs to the author of the package.";

    public static final String cordAppContractsPkg    = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg        = "it.nextworks.corda.flows";
    public static final String cordAppFinance         = "net.corda.finance.contracts";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name            = "O=DevTest,L=Pisa,C=IT";
    public static final String buyerX500Name          = "O=BuyerTest,L=Pistoia,C=IT";
    public static final String repositoryX500Name     = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String httpRequest            = "http://10.30.6.21:8083/";
    public static final String buyPkgOutputCashErr    = "There should be only outputs of type Cash.State " +
            "and PkgLicenseState";
}
