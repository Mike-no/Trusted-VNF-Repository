package it.nextworks.corda.contracts;

import it.nextworks.corda.states.FeeAgreementState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import static it.nextworks.corda.contracts.FeeAgreementUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class FeeAgreementContract implements Contract {

    /** This is used to identify our contract when building a transaction. */
    public static final String ID = "it.nextworks.corda.contracts.FeeAgreementContract";

    /**
     * A transaction is valid if the verify() function of the contract of all the
     * transaction's input and output states does not throw an exception.
     * @param tx transaction that has to be verified
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<FeeAgreementContract.Commands> command =
                requireSingleCommand(tx.getCommands(), FeeAgreementContract.Commands.class);
        final FeeAgreementContract.Commands commandData = command.getValue();

        if(commandData instanceof Commands.EstablishFeeAgreement) {
            requireThat(require -> {
                require.using(createAgreementInputErr, tx.getInputs().isEmpty());
                require.using(createAgreementOutputErr, tx.getOutputStates().size() == 1);

                final FeeAgreementState output = tx.outputsOfType(FeeAgreementState.class).get(0);

                final Party developer = output.getDeveloper();
                final Party repositoryNode = output.getRepositoryNode();
                require.using(nullDeveloper, developer != null);
                require.using(nullRepositoryNode, repositoryNode != null);
                require.using(sameEntityErr, !developer.equals(repositoryNode));

                final List<PublicKey> requiredSigners = command.getSigners();
                require.using(twoSignersErr, requiredSigners.size() == 2);

                final List<PublicKey> expectedSigners = Arrays.asList(developer.getOwningKey(),
                        repositoryNode.getOwningKey());
                require.using(mustBeSignersErr, requiredSigners.containsAll(expectedSigners));

                return null;
            });
        }
        else
            throw new IllegalArgumentException(unknownCommand);
    }

    public interface Commands extends CommandData {
        /**
         * Command used to establish a fee agreement between a developer and the repository
         * node: the result of this action is the creation of a FeeAgreementState that will be
         * saved in the vault of each participants.
         */
        class EstablishFeeAgreement implements FeeAgreementContract.Commands {}
    }
}
