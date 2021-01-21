package it.nextworks.corda.states;

import it.nextworks.corda.contracts.FeeAgreementContract;
import it.nextworks.corda.schemas.FeeAgreementSchemaV1;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(FeeAgreementContract.class)
public class FeeAgreementState implements ContractState, QueryableState {

    private final int fee;

    private final Party developer;
    private final Party repositoryNode;

    /**
     * Constructor of the Fee Agreement representation
     * @param developer      developer / author that want su submit a package in the marketplace
     * @param repositoryNode Repository Node that will store this FeeAgreementState in the vault
     */
    public FeeAgreementState(int fee, Party developer, Party repositoryNode) {
        this.fee = fee;

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

    @NotNull
    @Override
    public PersistentState generateMappedObject(@NotNull MappedSchema schema) {
        if(schema instanceof FeeAgreementSchemaV1)
            return new FeeAgreementSchemaV1.PersistentFeeAgreementState(fee, developer.getName().toString(),
                    repositoryNode.getName().toString());
        else
            throw new IllegalArgumentException("Unrecognised schema " + schema);
    }

    @NotNull
    @Override
    public Iterable<MappedSchema> supportedSchemas() { return Arrays.asList(new FeeAgreementSchemaV1()); }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("fee", fee)
                .append("developer", developer.getName().toString())
                .append("repository", repositoryNode.getName().toString())
                .toString();
    }
}
