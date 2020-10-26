package it.nextworks.corda.contracts;

import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import static it.nextworks.corda.contracts.PkgOfferUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class PkgOfferContract implements Contract {

    /** This is used to identify our contract when building a transaction. */
    public static final String ID = "it.nextworks.corda.contracts.PkgOfferContract";

    /**
     * A transaction is valid if the verify() function of the contract of all the
     * transaction's input and output states does not throw an exception.
     * @param tx transaction that has to be verified
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if(commandData instanceof Commands.RegisterPkg) {
            requireThat(require -> {
                require.using(createPkgInputErr, tx.getInputs().isEmpty());
                require.using(createPkgOutputErr, tx.getOutputStates().size() == 1);

                final PkgOfferState output = tx.outputsOfType(PkgOfferState.class).get(0);
                /* require.using(linearId + strNullErr, output.getLinearId() != null); is always true */

                require.using(name + strErrMsg, isWellFormatted(output.getName()));
                require.using(description + strErrMsg, isWellFormatted(output.getDescription()));
                require.using(version + strErrMsg, isWellFormatted(output.getVersion()));
                require.using(pkgInfoId + strErrMsg, isWellFormatted(output.getPkgInfoId()));
                try {
                    new URL(output.getImageLink());
                } catch (MalformedURLException mue) {
                    throw new IllegalArgumentException(imageLink + strMueErr);
                }
                require.using(pkgTypeErr, output.getPkgType() != null);
                require.using(poPrice + strNullErr, output.getPoPrice() != null);

                final Party author = output.getAuthor();
                final Party repositoryNode = output.getRepositoryNode();
                require.using(PkgOfferUtils.author + strNullErr, author != null);
                require.using(PkgOfferUtils.repositoryNode + strNullErr, repositoryNode != null);
                require.using(sameEntityErr, !author.equals(repositoryNode));

                final List<PublicKey> requiredSigners = command.getSigners();
                require.using(twoSignersErr, requiredSigners.size() == 2);

                final List<PublicKey> expectedSigners = Arrays.asList(author.getOwningKey(),
                        repositoryNode.getOwningKey());
                require.using(mustBeSignersErr, requiredSigners.containsAll(expectedSigners));

                return null;
            });
        }
        else if(commandData instanceof Commands.UpdatePkg) {
            requireThat(require -> {
                final List<ContractState> inputs = tx.getInputStates();
                require.using(updatePkgInputErr, inputs.size() == 1);
                require.using(updatePkgInputTypeErr, inputs.get(0) instanceof PkgOfferState);
                final PkgOfferState inputPkgOfferState = tx.inputsOfType(PkgOfferState.class).get(0);

                final List<ContractState> outputs = tx.getOutputStates();
                require.using(updatePkgOutputErr, outputs.size() == 1);
                require.using(updatePkgOutputTypeErr, outputs.get(0) instanceof PkgOfferState);
                final PkgOfferState outputPkgOfferState = tx.outputsOfType(PkgOfferState.class).get(0);

                require.using(updateLinearIdErr, inputPkgOfferState.getLinearId().equals(outputPkgOfferState.getLinearId()));

                require.using(name + strErrMsg, isWellFormatted(outputPkgOfferState.getName()));
                require.using(description + strErrMsg, isWellFormatted(outputPkgOfferState.getDescription()));
                require.using(version + strErrMsg, isWellFormatted(outputPkgOfferState.getVersion()));
                require.using(pkgInfoId + strErrMsg, isWellFormatted(outputPkgOfferState.getPkgInfoId()));
                try {
                    new URL(outputPkgOfferState.getImageLink());
                } catch (MalformedURLException mue) {
                    throw new IllegalArgumentException(imageLink + strMueErr);
                }
                require.using(updatePkgTypeErr, inputPkgOfferState.getPkgType().equals(outputPkgOfferState.getPkgType()));
                require.using(poPrice + strNullErr, outputPkgOfferState.getPoPrice() != null);

                final Party author = outputPkgOfferState.getAuthor();
                final Party repositoryNode = outputPkgOfferState.getRepositoryNode();
                require.using(updateAuthorErr, inputPkgOfferState.getAuthor().equals(author));
                require.using(updateRepositoryNodeErr, inputPkgOfferState.getRepositoryNode().equals(repositoryNode));

                final List<PublicKey> requiredSigners = command.getSigners();
                require.using(twoSignersErr, requiredSigners.size() == 2);

                final List<PublicKey> expectedSigners = Arrays.asList(author.getOwningKey(),
                        repositoryNode.getOwningKey());
                require.using(mustBeSignersErr, requiredSigners.containsAll(expectedSigners));

                return null;
            });
        }
        else if(commandData instanceof Commands.DeletePkg) {
            // TODO
            System.out.println("Not Implemented.");
        }
        else
            throw new IllegalArgumentException(unknownCommand);
    }

    /**
     * Used to indicate the transaction's intent.
     * All the following commands will be used by a developer to communicate
     * with the repository node.
     */
    public interface Commands extends CommandData {
        /**
         * Command used to create a new PkgOfferState, the latter will be stored in the
         * vaults of the two participants that are involved in the transaction where this
         * command is used.
         */
        class RegisterPkg implements Commands {}

        /**
         * Command used to update a PkgOfferState by sign the current PkgOfferState, used as input in the
         * current transaction, as CONSUMED and create a new output PkgOfferState.
         */
        class UpdatePkg implements Commands {}

        /**
         * Command used to delete a PkgOfferState by simply sign the PkgOfferState, used as input in the
         * current transaction, ad CONSUMED.
         */
        class DeletePkg implements Commands {}
    }

    private boolean isWellFormatted(String str){
        return str != null && !str.isEmpty() && str.trim().length() > 0;
    }
}