package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.CashIssueFlow;
import java.util.Currency;

@InitiatingFlow
@StartableByRPC
public class SelfIssueCashFlow extends FlowLogic<Cash.State> {

    private final Amount<Currency> amount;

    /**
     * Constructor of the class SelfIssueCashFlow
     * This class was retrieved by the corda sample flows.
     * @param amount the amount that the user want to self issue
     */
    public SelfIssueCashFlow(Amount<Currency> amount) { this.amount = amount; }

    @Suspendable
    public Cash.State call() throws FlowException {
        /* Create the cash issue command. */
        OpaqueBytes issueRef = OpaqueBytes.of("1".getBytes());

        final Party notary =
                getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse(BuyVnfFlowUtils.notaryX500Name));

        /* Create the cash issuance transaction. */
        SignedTransaction cashIssueTransaction = subFlow(new CashIssueFlow(amount, issueRef, notary)).getStx();
        /* Return the cash output. */
        return (Cash.State) cashIssueTransaction.getTx().getOutputs().get(0).getData();
    }
}