package it.nextworks.corda.states;

import com.sun.istack.NotNull;
import it.nextworks.corda.contracts.PkgOfferContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.util.Arrays;
import java.util.Currency;
import java.util.List;

@BelongsToContract(PkgOfferContract.class)
public class PkgOfferState implements LinearState {

    @CordaSerializable
    public enum PkgType {
        VNF,
        PNF
    }

    private final UniqueIdentifier linearId;

    private final String name;
    private final String description;
    private final String version;
    private final String pkgInfoId;
    private final String imageLink;
    private final Amount<Currency> price;
    private final PkgType pkgType;

    private final Party author;
    private final Party repositoryNode;

    /**
     * Constructor of the package State representation
     * @param linearId       LinearState models shared facts for which there is only
     *                       one current version at any point in time
     * @param name           name of this package
     * @param description    description of this package
     * @param version        version of this package
     * @param imageLink      customized marketplace cover art location of this package
     * @param pkgInfoId      id of package repository of this package
     * @param price          price of this package
     * @param pkgType        package type: VNF or PNF
     * @param author         author of this package
     * @param repositoryNode Repository Node that will store this PkgOfferState in the vault
     */
    public PkgOfferState(UniqueIdentifier linearId,
                         String name,
                         String description,
                         String version,
                         String pkgInfoId,
                         String imageLink,
                         Amount<Currency> price,
                         PkgType pkgType,
                         Party author,
                         Party repositoryNode) {
        this.linearId       = linearId;

        this.name           = name;
        this.description    = description;
        this.version        = version;
        this.pkgInfoId      = pkgInfoId;
        this.imageLink      = imageLink;
        this.price          = price;        /* This will become a ProductOfferingPrice */
        this.pkgType        = pkgType;

        this.author         = author;
        this.repositoryNode = repositoryNode;
    }

    /** Getters */

    @Override
    public UniqueIdentifier getLinearId() { return linearId; }

    public String getName(){
        return name;
    }

    public String getDescription() { return description; }

    public String getVersion() { return version; }

    public String getPkgInfoId() { return pkgInfoId; }

    public String getImageLink() { return imageLink; }

    public Amount<Currency> getPrice() { return price; }

    public PkgType getPkgType() { return pkgType; }

    public Party getAuthor() { return author; }

    public Party getRepositoryNode() { return repositoryNode; }

    /**
     * This method will indicate who are the participants and required signers when
     * this state is used in a transaction.
     * @return list of participants
     */
    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(author, repositoryNode);
    }
}