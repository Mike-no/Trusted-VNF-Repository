package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import it.nextworks.corda.schemas.PkgOfferSchemaV1;
import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.*;

import java.util.ArrayList;
import java.util.Currency;
import java.util.List;

import static it.nextworks.corda.flows.GetPkgsFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.*;

public class GetFilteredPkgsFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class GetFilteredPkgsInfoInitiation extends FlowLogic<List<PkgOfferState>> {

        private final Query query;

        public GetFilteredPkgsInfoInitiation(Query query) { this.query = query; }

        @Suspendable
        @Override
        public List<PkgOfferState> call() throws FlowException {

            /* Retrieving the Repository Node identity to request packages info */
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            /* Initiate the communication with the repository node and send the query to be performed */
            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);
            repositoryNodeSession.send(buildQueryCriteria());

            /* Receive and validate the package info container */
            PkgsInfoContainer pkgsInfoContainer =
                    repositoryNodeSession.receive(PkgsInfoContainer.class).unwrap(data -> {
                        List<PkgOfferState> lst = data.getPkgsList();
                        requireThat(require ->{
                            require.using(nullContainerErr, lst != null);
                            for(PkgOfferState pkgOfferState : lst)
                                require.using(nullEntryInContainer, pkgOfferState != null);

                            return null;
                        });

                        return data;
                    });

            return pkgsInfoContainer.getPkgsList();
        }

        @Suspendable
        private QueryCriteria buildQueryCriteria() {
            QueryCriteria criteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED);

            try {
                if(query.getLinearId() != null) {
                    FieldInfo attributeLinearId =
                            getField("linearId", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.equal(attributeLinearId,
                                    query.getLinearId())));
                }
                if(isWellFormatted(query.getName())) {
                    FieldInfo attributeName = getField("name", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.like(attributeName,
                                    "%" + query.getName() + "%", false)));
                }
                if(isWellFormatted(query.getDescription())) {
                    FieldInfo attributeDescription =
                            getField("description", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.like(attributeDescription,
                                    "%" + query.getDescription() + "%", false)));
                }
                if(isWellFormatted(query.getVersion())) {
                    FieldInfo attributeVersion =
                            getField("version", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.equal(attributeVersion,
                                    query.getVersion())));
                }
                if(query.getValue() != null) {
                    FieldInfo attributeValue =
                            getField("value", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.lessThanOrEqual(attributeValue,
                                    query.getValue())));
                }
                if(query.getUnit() != null) {
                    String unit = query.getUnit();
                    Currency.getInstance(unit);
                    FieldInfo attributeUnit = getField("unit", PkgOfferSchemaV1.PersistentPkgOfferState.class);

                    criteria = criteria
                            .and(new QueryCriteria.VaultCustomQueryCriteria(Builder.equal(attributeUnit, unit)));
                }
            } catch (NoSuchFieldException e) {
                throw new IllegalArgumentException(malformedDbTable);
            }

            return criteria;
        }
    }

    @InitiatedBy(GetFilteredPkgsFlow.GetFilteredPkgsInfoInitiation.class)
    public static class RepositoryNodeAcceptor extends FlowLogic<Void> {

        private final FlowSession userSession;

        /**
         * Constructor of the flow initiated by the GetPkgsInfoInitiation class
         * @param userSession session with the user that want to explore the packages
         */
        public RepositoryNodeAcceptor(FlowSession userSession) { this.userSession = userSession; }

        @Suspendable
        @Override
        public Void call() throws FlowException {
            QueryCriteria criteria = userSession.receive(QueryCriteria.class).unwrap(data -> {
                if(data == null)
                    throw new IllegalArgumentException(notQueryCriteriaRcv);

                return data;
            });

            /*
             * Query for states that match the received criteria using a pagination specification and iterate
             * using the totalStatesAvailable field until no further pages available
             */
            int pageNumber = DEFAULT_PAGE_NUM;
            List<StateAndRef<PkgOfferState>> states = new ArrayList<>();
            long totalResults;
            do {
                PageSpecification pageSpecification = new PageSpecification(pageNumber, DEFAULT_PAGE_SIZE);
                Vault.Page<PkgOfferState> results =
                        getServiceHub().getVaultService()
                                .queryBy(PkgOfferState.class, criteria, pageSpecification);
                totalResults = results.getTotalStatesAvailable();
                states.addAll(results.getStates());
                pageNumber++;
            } while((DEFAULT_PAGE_SIZE * (pageNumber - 1) <= totalResults));

            List<PkgOfferState> pkgOfferStateList = new ArrayList<>();
            for(StateAndRef<PkgOfferState> pkgOfferStateAndRef : states)
                pkgOfferStateList.add(pkgOfferStateAndRef.getState().getData());

            PkgsInfoContainer pkgsInfoContainer = new PkgsInfoContainer(pkgOfferStateList);
            userSession.send(pkgsInfoContainer);

            return null;
        }
    }
}
