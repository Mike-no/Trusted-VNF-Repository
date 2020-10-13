package it.nextworks.corda.flows;

public class BuyVnfFlowUtils {
    public static final String receivedTooMuchStates  = "Received more than one VnfState: " +
            "require only the requested VnfState";
    public static final String requestedVnfErr        = "The received VNF differ from the requested one.";
    public static final String priceMismatch          = "The received VNF price differ from the displayed one";
    public static final String repositoryNodeMismatch = "The received VNF specify a different Repository Node.";
    public static final String notUniqueIdentifier    = "The received data it is not a UniqueIdentifier.";
    public static final String nonExistentVnf         = "Cannot find VNF: ";
    public static final String unexpectedInvalidPrice = "The received amount for the payment does not match the VNF price";

    public static final String SENDING_VNF_ID         = "Sending the ID of the required VNF.";
    public static final String RECEIVING_VNF_INFO     = "Receiving information about the VNF to buy.";
    public static final String VERIFYING_VNF_INFO     = "Verifying the information received about the VNF.";
    public static final String GENERATING_TRANSACTION = "Generating transaction based on the VNF information.";
    public static final String VERIFYING_TRANSACTION  = "Verifying contract constraints.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String GATHERING_SIGNS        = "Gathering the Repository Node's signature.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String AWAITING_VNF_ID        = "Waiting for the ID of the VNF that the buyer want to purchase.";
    public static final String VERIFYING_RCV_DATA     = "Verifying the information received.";
    public static final String SENDING_VNF_INFO       = "Sending information about the required VNF.";
}
