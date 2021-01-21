package it.nextworks.corda.schemas;

import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.Arrays;
//4.6 changes
import javax.annotation.Nullable;

public class FeeAgreementSchemaV1 extends MappedSchema {

    public FeeAgreementSchemaV1() {
        super(FeeAgreementSchema.class, 1, Arrays.asList(PersistentFeeAgreementState.class));
    }

    @Entity
    @Table(name = "fee_agreement_states")
    public static class PersistentFeeAgreementState extends PersistentState {

        @Column(name = "fee") private final int fee;
        @Column(name = "developer") private final String developer;
        @Column(name = "repository") private final String repository;

        /**
         * Constructor of the PersistentFeeAgreementState class, used for schema build
         * @param fee        fee of this agreement
         * @param developer  developer involved in this fee agreement
         * @param repository repository involved in this fee agreement
         */
        public PersistentFeeAgreementState(int fee, String developer, String repository) {
            this.fee        = fee;
            this.developer  = developer;
            this.repository = repository;
        }

        /* Default constructor required by hibernate */
        public PersistentFeeAgreementState() {
            fee        = 0;
            developer  = null;
            repository = null;
        }

        /* Getters */

        public int getFee() { return fee; }

        public String getDeveloper() { return developer; }

        public String getRepository() { return repository; }
    }

    @Nullable
    @Override
    public String getMigrationResource() { return "fee_agreement.changelog-master"; }
}
