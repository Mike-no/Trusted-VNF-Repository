package it.nextworks.corda.flows;

import co.paralleluniverse.fibers.Suspendable;
import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.PageSpecification;
import net.corda.core.node.services.vault.QueryCriteria.VaultQueryCriteria;

import java.util.ArrayList;
import java.util.List;

import static it.nextworks.corda.flows.GetPkgsFlowUtils.*;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_NUM;
import static net.corda.core.node.services.vault.QueryCriteriaUtils.DEFAULT_PAGE_SIZE;

public class GetPkgsFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class GetPkgsInfoInitiation extends FlowLogic<List<PkgOfferState>> {

        @Suspendable
        @Override
        public List<PkgOfferState> call() throws FlowException {

            /* Retrieving the Repository Node identity to request packages info */
            final Party repositoryNode = getServiceHub()
                    .getNetworkMapCache()
                    .getPeerByLegalName(CordaX500Name.parse(repositoryX500Name));

            FlowSession repositoryNodeSession = initiateFlow(repositoryNode);

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
    }

    @InitiatedBy(GetPkgsInfoInitiation.class)
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
            /*
             * Query for all states using a pagination specification and iterate
             * using the totalStatesAvailable field until no further pages available
             */
            int pageNumber = DEFAULT_PAGE_NUM;
            List<StateAndRef<PkgOfferState>> states = new ArrayList<>();
            long totalResults;
            do {
                PageSpecification pageSpecification = new PageSpecification(pageNumber, DEFAULT_PAGE_SIZE);
                Vault.Page<PkgOfferState> results =
                        getServiceHub().getVaultService()
                                .queryBy(PkgOfferState.class, new VaultQueryCriteria()
                                        .withStatus(Vault.StateStatus.UNCONSUMED), pageSpecification);
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
