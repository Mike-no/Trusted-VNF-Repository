package it.nextworks.corda.states;

import it.nextworks.corda.contracts.VnfLicenseContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(VnfLicenseContract.class)
public class VnfLicenseState implements ContractState {

    private final StateAndRef<VnfState> vnfLicensed;
    private final String repositoryLink;
    private final int repositoryHash;

    private final Party buyer;
    private final Party repositoryNode;

    /**
     * Constructor of the VNF License State representation
     * @param vnfLicensed    reference to the vnf State sold
     * @param repositoryLink link to package repository (package location) of the VNF sold
     * @param repositoryHash hash of the repository link of the VNF sold
     * @param buyer          the user who bought the vnf associated to this license
     * @param repositoryNode Repository Node that will store this VnfLicenseState in the vault
     */
    public VnfLicenseState(StateAndRef<VnfState> vnfLicensed, String repositoryLink,
                           int repositoryHash, Party buyer, Party repositoryNode) {
        this.vnfLicensed    = vnfLicensed;
        this.repositoryLink = repositoryLink;
        this.repositoryHash = repositoryHash;
        this.buyer          = buyer;
        this.repositoryNode = repositoryNode;
    }

    /** Getters */

    public StateAndRef<VnfState> getVnfLicensed() {
        return vnfLicensed;
    }

    public String getRepositoryLink() {
        return repositoryLink;
    }

    public int getRepositoryHash() {
        return repositoryHash;
    }

    public Party getBuyer() {
        return buyer;
    }

    public Party getRepositoryNode() {
        return repositoryNode;
    }

    /**
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * @return list of participants
     */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(buyer, repositoryNode);
    }
}
