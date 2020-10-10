package it.nextworks.corda.contracts;

import it.nextworks.corda.states.VnfLicenseState;
import it.nextworks.corda.states.VnfState;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.contracts.VnfUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.contracts.utils.StateSumming.sumCashBy;

public class VnfLicenseContract implements Contract {

    /** This is used to identify our contract when building a transaction. */
    public static final String ID = "it.nextworks.corda.contracts.VnfLicenseContract";

    /**
     * A transaction is valid if the verify() function of the contract of all the
     * transaction's input and output states does not throw an exception.
     * @param tx transaction that has to be verified
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if(commandData instanceof Commands.BuyVNF) {
            /**
             * the purchase of a vnf (and the issuance of the relative license) requires
             * movement of on-ledger cash.
             */
            requireThat(require -> {
                /**
                 * The following controls on input and output cash states are not really
                 * needed (they should be always true), consider to remove them.
                 * ###################################################################
                 */
                int cashInputStateCount = 0;
                for(ContractState input : tx.getInputStates()) {
                    require.using(buyVnfInputCashErr, input instanceof Cash.State);
                    cashInputStateCount++;
                }
                require.using(buyVnfInputCashEmp, cashInputStateCount > 0);
                int vnfLicenseStateCount = 0;
                int cashOutputStateCount = 0;
                for(ContractState output : tx.getOutputStates()) {
                    if(output instanceof VnfLicenseState)
                        vnfLicenseStateCount++;
                    else if(output instanceof Cash.State)
                        cashOutputStateCount++;
                    else
                        throw new IllegalArgumentException(buyVnfOutputCashErr);
                }
                require.using(buyVnfOutputCashEmp, cashOutputStateCount > 0);
                require.using(buyVnfLicenseOutErr, vnfLicenseStateCount == 1);
                /** ################################################################## */

                final VnfLicenseState vnfLicenseState = tx.outputsOfType(VnfLicenseState.class).get(0);
                final VnfState vnfState = vnfLicenseState.getVnfLicensed().getState().getData();

                final String vnfStateRepositoryLink = vnfState.getRepositoryLink();
                require.using(repositoryLink + cannotDiffer,
                        vnfStateRepositoryLink.equals(vnfLicenseState.getRepositoryLink()));
                require.using(repositoryHashErr,
                        vnfStateRepositoryLink.hashCode() == vnfLicenseState.getRepositoryHash());

                final Party repositoryNode = vnfLicenseState.getRepositoryNode();
                final Amount<Issued<Currency>> receivedAmount =
                        sumCashBy(tx.getOutputStates(), repositoryNode);
                final Amount<Currency> vnfStatePrice = vnfState.getPrice();
                require.using(differentAmountErr, withoutIssuer(receivedAmount).equals(vnfStatePrice));

                final Party buyer = vnfLicenseState.getBuyer();
                require.using(VnfUtils.buyer + strNullErr, buyer != null);
                final Party vnfStateRepositoryNode = vnfState.getRepositoryNode();
                require.using(VnfUtils.repositoryNode + cannotDiffer,
                        repositoryNode.equals(vnfStateRepositoryNode));
                require.using(buyerSameIdentity, !buyer.equals(repositoryNode));

                final List<PublicKey> requiredSigners = command.getSigners();
                require.using(twoSignersErr, requiredSigners.size() == 2);

                final List<PublicKey> expectedSigners = Arrays.asList(buyer.getOwningKey(),
                        repositoryNode.getOwningKey());
                require.using(mustBeSignersLicErr, requiredSigners.containsAll(expectedSigners));

                return null;
            });
        }
        else
            throw new IllegalArgumentException(unknownCommand);
    }

    public interface Commands extends CommandData {
        /**
         * Command used to buy a VNF and so create a VnfLicenseState that will be
         * stored in the vaults of the two participants that are involved in the
         * transaction where this command is used.
         */
        class BuyVNF implements  Commands {}
    }
}
