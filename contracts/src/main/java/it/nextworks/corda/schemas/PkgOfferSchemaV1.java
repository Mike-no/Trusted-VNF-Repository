package it.nextworks.corda.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
import java.util.UUID;
//4.6 changes
import org.hibernate.annotations.Type;
import javax.annotation.Nullable;

public class PkgOfferSchemaV1 extends MappedSchema {

    public PkgOfferSchemaV1() {
        super(PkgOfferSchema.class, 1, Arrays.asList(PersistentPkgOfferState.class));
    }

    @Entity
    @Table(name = "pkg_offer_states")
    public static class PersistentPkgOfferState extends PersistentState {

        @Column(name = "linear_id") @Type (type = "uuid-char") private final UUID linearId;
        @Column(name = "name") private final String name;
        @Column(name = "description") private final String description;
        @Column(name = "version") private final String version;
        @Column(name = "value") private final double value;
        @Column(name = "unit") private final String unit;

        /**
         * Constructor of the PersistentPkgOfferState class, used for the schema build
         * @param linearId    linearId of this package offer
         * @param name        name of this package offer
         * @param description description of this package offer
         * @param version     version of this package offer
         * @param value       price of this package offer
         * @param unit        currency for the price of this package offer
         */
        public PersistentPkgOfferState(UUID linearId,
                                       String name,
                                       String description,
                                       String version,
                                       double value,
                                       String unit) {
            this.linearId    = linearId;
            this.name        = name;
            this.description = description;
            this.version     = version;
            this.value       = value;
            this.unit        = unit;
        }

        /* Default constructor required by hibernate */
        public PersistentPkgOfferState() {
            linearId    = null;
            name        = null;
            description = null;
            version     = null;
            value       = 0;
            unit        = null;
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

        public double getValue() {
            return value;
        }

        public String getUnit() {
            return unit;
        }
    }

    @Nullable
    @Override
    public String getMigrationResource() {
        return "pkg_offer.changelog-master";
    }
}
