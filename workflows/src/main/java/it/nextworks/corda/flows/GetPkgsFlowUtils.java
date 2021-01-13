package it.nextworks.corda.flows;

import it.nextworks.corda.states.PkgOfferState;
import net.corda.core.serialization.CordaSerializable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class GetPkgsFlowUtils {

    @CordaSerializable
    public static class PkgsInfoContainer {

        private final List<PkgOfferState> pkgsList;

        /**
         * Constructor of the PkgsInfoContainer class
         * @param pkgsList list of PkgOfferState
         */
        public PkgsInfoContainer(List<PkgOfferState> pkgsList) { this.pkgsList = pkgsList; }

        /* Getter */

        public List<PkgOfferState> getPkgsList() { return pkgsList; }
    }

    @CordaSerializable
    public static class Query {

        private final UUID linearId;
        private final String name;
        private final String description;
        private final String version;
        private final BigDecimal value;
        private final String unit;

        /**
         * Constructor of the Query class, used by the QueryBuilder
         * @param linearId    ID of the package to retrieve from the marketplace
         * @param name        name of the package(s) to retrieve from the marketplace
         * @param description description of the package(s) to retrieve from the marketplace
         * @param version     version of the package(s) to retrieve from the marketplace
         * @param value       price of the package(s) to retrieve from the marketplace
         * @param unit        currency of the package(s) to retrieve from the marketplace
         */
        public Query(UUID linearId,
                     String name,
                     String description,
                     String version,
                     BigDecimal value,
                     String unit) {
            this.linearId    = linearId;
            this.name        = name;
            this.description = description;
            this.version     = version;
            this.value       = value;
            this.unit        = unit;
        }

        /* Getters */

        public UUID getLinearId() { return linearId; }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public String getVersion() {
            return version;
        }

        public BigDecimal getValue() {
            return value;
        }

        public String getUnit() {
            return unit;
        }
    }

    @CordaSerializable
    public static class QueryBuilder {

        private UUID linearId      = null;
        private String name        = null;
        private String description = null;
        private String version     = null;
        private BigDecimal value   = null;
        private String unit        = null;

        /* Setters */

        public QueryBuilder setLinearId(UUID linearId) {
            this.linearId = linearId;
            return this;
        }

        public QueryBuilder setName(String name) {
            this.name = name;
            return this;
        }

        public QueryBuilder setDescription(String description) {
            this.description = description;
            return this;
        }

        public QueryBuilder setVersion(String version) {
            this.version = version;
            return this;
        }

        public QueryBuilder setValue(BigDecimal value) {
            this.value = value;
            return this;
        }

        public QueryBuilder setUnit(String unit) {
            this.unit = unit;
            return this;
        }

        public Query build() { return new Query(linearId, name, description, version, value, unit); }
    }

    public static final String nullContainerErr     = "The received container is null.";
    public static final String nullEntryInContainer = "The received container contains null VNF info entry.";

    public static final String malformedDbTable     = "The Database table cannot be used: malformed column(s).";
    public static final String notQueryCriteriaRcv  = "The received data is null.";

    public static final String notaryX500Name       = "O=Notary,L=Pisa,C=IT";
    public static final String devX500Name          = "O=DevTest,L=Pisa,C=IT";
    public static final String buyerX500Name        = "O=BuyerTest,L=Pistoia,C=IT";
    public static final String repositoryX500Name   = "O=RepositoryNode,L=Pisa,C=IT";

    public static final String cordAppContractsPkg  = "it.nextworks.corda.contracts";
    public static final String cordAppFlowsPkg      = "it.nextworks.corda.flows";

    public static boolean isWellFormatted(String str){
        return str != null && !str.isEmpty() && str.trim().length() > 0;
    }
}
