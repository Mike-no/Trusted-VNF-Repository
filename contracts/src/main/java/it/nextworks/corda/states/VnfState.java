package it.nextworks.corda.states;

import com.sun.istack.NotNull;
import it.nextworks.corda.contracts.VnfContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;

import java.net.URL;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

@BelongsToContract(VnfContract.class)
public class VnfState implements LinearState {

    private final UniqueIdentifier linearId;

    private final String name;
    private final String description;
    private final String serviceType;
    private final String version;
    private final String requirements;
    private final String resources;
    private final String imageLink;
    private final String repositoryLink;
    private final int repositoryHash;
    private final Amount<Currency> price;

    private final Party author;
    private final Party repositoryNode;

    /**
     * Constructor of the VNF State representation
     * @param linearId LinearState models shared facts for which there is only
     *                 one current version at any point in time
     * @param name name of this VNF
     * @param description description of this VNF
     * @param serviceType identify the service type of this VNF
     * @param version version of this VNF
     * @param requirements list of requirements needed by this VNF
     * @param resources resources needed by this VNF
     * @param imageLink customized marketplace cover art location of this VNF
     * @param repositoryLink link to package repository (package location) of this VNF
     * @param repositoryHash hash of the repository link of this VNF
     * @param price price of this VNF
     * @param author author of this VNF
     * @param repositoryNode Repository Node that will store this VnfState in the vault
     */
    public VnfState(UniqueIdentifier linearId, String name, String description,
                    String serviceType, String version, String requirements,
                    String resources, String imageLink, String repositoryLink,
                    int repositoryHash, Amount<Currency> price, Party author,
                    Party repositoryNode) {
        this.linearId = linearId;
        this.name = name;
        this.description = description;
        this.serviceType = serviceType;
        this.version = version;
        this.requirements = requirements;
        this.resources = resources;
        this.imageLink = imageLink;
        this.repositoryLink = repositoryLink;
        this.repositoryHash = repositoryHash;
        this.price = price;
        this.author = author;
        this.repositoryNode = repositoryNode;
    }

    /** Getters */

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    public String getName(){
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getServiceType() {
        return serviceType;
    }

    public String getVersion() {
        return version;
    }

    public String getRequirements() {
        return requirements;
    }

    public String getResources() {
        return resources;
    }

    public String getImageLink() {
        return imageLink;
    }

    public String getRepositoryLink() {
        return repositoryLink;
    }

    public int getRepositoryHash() {
        return repositoryHash;
    }

    public Party getAuthor() {
        return author;
    }

    public Party getRepositoryNode() {
        return repositoryNode;
    }

    public Amount<Currency> getPrice() {
        return price;
    }

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