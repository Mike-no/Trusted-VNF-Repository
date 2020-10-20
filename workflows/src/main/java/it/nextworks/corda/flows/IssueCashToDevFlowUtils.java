package it.nextworks.corda.flows;

public class IssueCashToDevFlowUtils {
    public static final String SENDING_LICENSE        = "Sending the license of the sold package.";
    public static final String GENERATING_TRANSACTION = "Generating transaction.";
    public static final String SIGNING_TRANSACTION    = "Signing transaction with our private key.";
    public static final String FINALISING_TRANSACTION = "Obtaining Notary signature and recording transaction.";

    public static final String notaryX500Name         = "O=Notary,L=Pisa,C=IT";
}
