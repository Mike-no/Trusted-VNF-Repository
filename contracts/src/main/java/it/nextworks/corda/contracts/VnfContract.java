package it.nextworks.corda.contracts;

import it.nextworks.corda.states.VnfState;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;

import java.net.URL;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.List;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class VnfContract implements Contract {

    /** This is used to identify our contract when building a transaction. */
    public static final String ID = "com.template.contracts.VnfContract";

    private static final String strErrMsg  = " parameter cannot be null, empty or only composed by whitespace";
    private static final String strNullErr = " parameter cannot be null";

    /**
     * A transaction is valid if the verify() function of the contract of all the
     * transaction's input and output states does not throw an exception.
     * @param tx transaction that has to be verified
     */
    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();

        if(commandData instanceof Commands.CreateVNF) {
            requireThat(require -> {
                require.using("No input should be consumed when create a VNF", tx.getInputs().isEmpty());
                require.using("There should be one output state of type VnfState",
                        tx.getOutputStates().size() == 1);

                final VnfState output = tx.outputsOfType(VnfState.class).get(0);
                require.using("The <linearId>" + strNullErr, output.getLinearId() != null);

                require.using("The <name>" + strErrMsg, isWellFormatted(output.getName()));
                require.using("The <description>" + strErrMsg, isWellFormatted(output.getDescription()));
                require.using("The <serviceType>" + strErrMsg, isWellFormatted(output.getServiceType()));
                require.using("The <version>" + strErrMsg, isWellFormatted(output.getVersion()));
                require.using("The <requirements>" + strErrMsg, isWellFormatted(output.getRequirements()));
                require.using("The <resources>" + strErrMsg, isWellFormatted(output.getResources()));
                require.using("The <imageLink>" + strNullErr, output.getImageLink() != null);
                URL repositoryLink = output.getRepositoryLink();
                require.using("The <repositoryLink>" + strNullErr, repositoryLink != null);
                require.using("The <repositoryHash> parameter does not match the hash value of" +
                        "the <repositoryLink> parameter",
                        output.getRepositoryHash() == repositoryLink.hashCode());
                require.using("The <price>" + strNullErr, output.getPrice() != null);

                final Party author = output.getAuthor();
                final Party repositoryNode = output.getRepositoryNode();
                require.using("The <author>" + strNullErr, author != null);
                require.using("The <repositoryNode>" + strNullErr, repositoryNode != null);
                require.using("The <author> parameter and the <repositoryNode> parameter cannot be" +
                        "the same entity", !author.equals(repositoryNode));

                final List<PublicKey> requiredSigners = command.getSigners();
                require.using("There must be two signers", requiredSigners.size() == 2);

                final List<PublicKey> expectedSigners = Arrays.asList(author.getOwningKey(),
                        repositoryNode.getOwningKey());
                require.using("<author> and <repositoryNode> must be signers",
                        requiredSigners.containsAll(expectedSigners));

                return null;
            });
        }
        else if(commandData instanceof Commands.UpdateVNF) {
            // TODO
        }
        else if(commandData instanceof  Commands.DeleteVNF) {
            // TODO
        }
        else
            throw new IllegalArgumentException("Unrecognised command");
    }

    /**
     * Used to indicate the transaction's intent.
     * All the following commands will be used by a developer to communicate
     * with the repository node.
     */
    public interface Commands extends CommandData {
        /**
         * Command used to create a new VnfState, the latter will be stored in the
         * vaults of the two participants that are involved in the transaction where this
         * command is used.
         */
        class CreateVNF implements Commands {}

        /**
         * Command used to update a VnfState by sign the current VnfState, used as input in the
         * current transaction, as CONSUMED and create a new output VnfState.
         */
        class UpdateVNF implements Commands {}

        /**
         * Command used to delete a VnfState by simply sign the VnfState, used as input in the
         * current transaction, ad CONSUMED.
         */
        class DeleteVNF implements Commands {}
    }

    private boolean isWellFormatted(String str){
        return str != null && !str.isEmpty() && str.trim().length() > 0;
    }
}