package it.nextworks.corda.contracts;

import it.nextworks.corda.states.PkgLicenseState;
import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.contracts.PkgLicenseUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.contracts.utils.StateSumming.sumCashBy;

public class PkgLicenseContract implements Contract {

    /** This is used to identify our contract when building a transaction. */
    public static final String ID = "it.nextworks.corda.contracts.PkgLicenseContract";

    /**
     * A transaction is valid if the verify() function of the contract of all the
     * transaction's input and output states does not throw an exception.
     * @param tx transaction that has to be verified
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if(commandData instanceof Commands.BuyPkg) {
            /**
             * the purchase of a package (and the issuance of the relative license) requires
             * movement of on-ledger cash.
             */
            requireThat(require -> {
                int cashInputStateCount = 0;
                for(ContractState input : tx.getInputStates()) {
                    require.using(buyPkgInputCashErr, input instanceof Cash.State);
                    cashInputStateCount++;
                }
                require.using(buyPkgInputCashEmp, cashInputStateCount > 0);
                int pkgLicenseStateCount = 0;
                int cashOutputStateCount = 0;
                for(ContractState output : tx.getOutputStates()) {
                    if(output instanceof PkgLicenseState)
                        pkgLicenseStateCount++;
                    else if(output instanceof Cash.State)
                        cashOutputStateCount++;
                    else
                        throw new IllegalArgumentException(buyPkgOutputCashErr);
                }
                require.using(buyPkgOutputCashEmp, cashOutputStateCount > 0);
                require.using(buyPkgLicenseOutErr, pkgLicenseStateCount == 1);

                final PkgLicenseState pkgLicenseState = tx.outputsOfType(PkgLicenseState.class).get(0);
                final PkgOfferState pkgOfferState = pkgLicenseState.getPkgLicensed().getState().getData();

                final Party repositoryNode = pkgOfferState.getRepositoryNode();
                final Amount<Issued<Currency>> receivedAmount =
                        sumCashBy(tx.getOutputStates(), repositoryNode);
                require.using(differentAmountErr, withoutIssuer(receivedAmount).equals(pkgOfferState.getPrice()));

                final Party buyer = pkgLicenseState.getBuyer();
                require.using(PkgLicenseUtils.buyer + strNullErr, buyer != null);
                require.using(buyerSameIdentity, !buyer.equals(repositoryNode));
                require.using(buyerAndAuthorSame, !buyer.equals(pkgOfferState.getAuthor()));

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
         * Command used to buy a package and so create a PkgLicenseState that will be
         * stored in the vaults of the two participants that are involved in the
         * transaction where this command is used.
         */
        class BuyPkg implements  Commands {}
    }
}
