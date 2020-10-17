package it.nextworks.corda.states;

import com.sun.istack.NotNull;
import it.nextworks.corda.contracts.FeeAgreementContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(FeeAgreementContract.class)
public class FeeAgreementState implements ContractState {

    private static final int fee = 10;

    private final Party developer;
    private final Party repositoryNode;

    /**
     * Constructor of the Fee Agreement representation
     * @param developer      developer / author that want su submit a package in the marketplace
     * @param repositoryNode Repository Node that will store this FeeAgreementState in the vault
     */
    public FeeAgreementState(Party developer, Party repositoryNode) {
        this.developer = developer;
        this.repositoryNode = repositoryNode;
    }

    /** Getters */

    public int getFee() { return fee; }

    public Party getDeveloper() { return developer; }

    public Party getRepositoryNode() { return repositoryNode; }

    /**
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * @return list of participants
     */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(developer, repositoryNode);
    }
}
