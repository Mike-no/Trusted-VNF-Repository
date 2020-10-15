package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import it.nextworks.corda.states.VnfState;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;
import net.corda.core.serialization.CordaSerializable;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.flows.GetVnfsFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_SIZE;

public class GetVnfsFlow {

    /**
     * Lightweight class used to send to the buyer only information
     * relevant when viewing the VNF marketplace.
     * The information that compound an instance of this class
     * are extract from a VnfState stored in the Repository Node Vault.
     */
    @CordaSerializable
    public static class VnfInfo {

        private final UniqueIdentifier vnfId;

        private final String name;
        private final String description;
        private final String serviceType;
        private final String version;
        private final String imageLink;
        private final Amount<Currency> price;

        private final Party author;

        /**
         * Constructor of the VnfInfo class
         * @param vnfId       ID of the VNF represented by a VnfState
         * @param name        name of the VNF represented by a VnfState
         * @param description description of the VNF represented by a VnfState
         * @param serviceType service type of the VNF represented by a VnfState
         * @param version     version of the VNF represented by a VnfState
         * @param imageLink   image link of the VNF represented by a VnfState
         * @param price       price of the VNF represented by a VnfState
         * @param author      author of the VNF represented by a VnfState
         */
        public VnfInfo(UniqueIdentifier vnfId, String name, String description, String serviceType,
                       String version, String imageLink, Amount<Currency> price, Party author) {
            this.vnfId       = vnfId;
            this.name        = name;
            this.description = description;
            this.serviceType = serviceType;
            this.version     = version;
            this.imageLink   = imageLink;
            this.price       = price;
            this.author      = author;
        }

        /* Getters */

        public UniqueIdentifier getVnfId() { return vnfId; }

        public String getName() { return name; }

        public String getDescription() { return description; }

        public String getServiceType() { return serviceType; }

        public String getVersion() { return version; }

        public String getImageLink() { return imageLink; }

        public Amount<Currency> getPrice() { return price; }

        public Party getAuthor() { return author; }
    }

    @CordaSerializable
    public static class VnfsInfoContainer {

        private final List<VnfInfo> vnfsInfoList;

        /**
         * Constructor of the VnfsInfoContainer class
         * @param vnfsInfoList list of VnfInfo objects
         */
        public VnfsInfoContainer(List<VnfInfo> vnfsInfoList) { this.vnfsInfoList = vnfsInfoList; }

        /* Getter */

        public List<VnfInfo> getVnfsInfoList() { return vnfsInfoList; }
    }

    @InitiatingFlow
    @StartableByRPC
    public static class GetVnfInfoInitiation extends FlowLogic<List<VnfInfo>> {

        @Suspendable
        @Override
        public List<VnfInfo> call() throws FlowException {

            /* Retrieving the Repository Node identity to request VNFs info */
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);

            /* Receive and validate the VNF info container */
            VnfsInfoContainer vnfsInfoContainer =
                repositoryNodeSession.receive(VnfsInfoContainer.class).unwrap(data -> {
                    List<VnfInfo> lst = data.getVnfsInfoList();
                    requireThat(require ->{
                        require.using(nullContainerErr, lst != null);
                        for(VnfInfo vnfInfo : lst)
                            require.using(nullEntryInContainer, vnfInfo != null);

                        return null;
                    });

                    return data;
                });

            return vnfsInfoContainer.getVnfsInfoList();
        }
    }

    @InitiatedBy(GetVnfsFlow.GetVnfInfoInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<Void> {

        private final FlowSession userSession;

        /**
         * Constructor of the flow initiated by the GetVnfInfoInitiation class
         * @param userSession session with the user that want to explore the VNFs
         */
        public RepositoryNodeAcceptor(FlowSession userSession) { this.userSession = userSession; }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            /*
             * Query for all states using a pagination specification and iterate
             * using the totalStatesAvailable field until no further pages available
             */
            int pageNumber = DEFAULT_PAGE_NUM;
            List<StateAndRef<VnfState>> states = new ArrayList<>();
            long totalResults;
            do {
                PageSpecification pageSpecification = new PageSpecification(pageNumber, DEFAULT_PAGE_SIZE);
                Vault.Page<VnfState> results =
                        getServiceHub().getVaultService()
                                .queryBy(VnfState.class, new VaultQueryCriteria(), pageSpecification);
                totalResults = results.getTotalStatesAvailable();
                states.addAll(results.getStates());
                pageNumber++;
            } while((DEFAULT_PAGE_SIZE * (pageNumber - 1) <= totalResults));

            /*
             * Extract the VnfStates from the StateAndRef<VnfState> objects and create the
             * correspondents VnfInfo objects. Wrap the resultant list of VnfInfo into the
             * VnfInfoContainer and send the latter to the user.
             */
            List<VnfInfo> vnfInfoList = new ArrayList<>();
            for(StateAndRef<VnfState> vnfStateAndRef : states) {
                VnfState vnfState = vnfStateAndRef.getState().getData();
                VnfInfo vnfInfo = new VnfInfo(vnfState.getLinearId(), vnfState.getName(), vnfState.getDescription(),
                        vnfState.getServiceType(), vnfState.getVersion(), vnfState.getImageLink(),
                        vnfState.getPrice(), vnfState.getAuthor());

                vnfInfoList.add(vnfInfo);
            }

            VnfsInfoContainer vnfsInfoContainer = new VnfsInfoContainer(vnfInfoList);
            userSession.send(vnfsInfoContainer);

            return null;
        }
    }
}
