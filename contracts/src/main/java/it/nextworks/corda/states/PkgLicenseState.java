package it.nextworks.corda.states;

import it.nextworks.corda.contracts.PkgLicenseContract;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;

@BelongsToContract(PkgLicenseContract.class)
public class PkgLicenseState implements ContractState {

    private final StateAndRef<PkgOfferState> pkgLicensed;
    private final Party buyer;

    /**
     * Constructor of the package License State representation
     * @param pkgLicensed    reference to the package State sold
     * @param buyer          the user who bought the package associated to this license
     */
    public PkgLicenseState(StateAndRef<PkgOfferState> pkgLicensed, Party buyer) {
        this.pkgLicensed = pkgLicensed;
        this.buyer       = buyer;
    }

    /** Getters */

    public StateAndRef<PkgOfferState> getPkgLicensed() {
        return pkgLicensed;
    }

    public Party getBuyer() {
        return buyer;
    }

    /**
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * @return list of participants
     */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(buyer, pkgLicensed.getState().getData().getRepositoryNode());
    }
}
