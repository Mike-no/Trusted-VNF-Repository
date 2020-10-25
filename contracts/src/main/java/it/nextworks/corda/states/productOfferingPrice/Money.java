package it.nextworks.corda.states.productOfferingPrice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.corda.core.serialization.CordaSerializable;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Currency;

@CordaSerializable
public class Money {

    @JsonProperty("unit") private final String unit;
    @JsonProperty("value") private final float value;

    /**
     * Constructor of the Money class.
     * @param unit  Currency (ISO4217)
     * @param value A positive floating point number
     */
    @JsonCreator
    public Money(@JsonProperty("unit") String unit, @JsonProperty("value") float value){
        Currency.getInstance(unit);

        if(value < 0.01)
            throw new IllegalArgumentException("The <value> parameter cannot be negative or zero.");

        this.unit = unit;
        this.value = value;
    }

    /* Getters */

    public String getUnit() { return unit; }

    public float getValue() { return value; }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof Money))
            return false;

        Money other = (Money)obj;

        if(!(this.unit.equals(other.unit)))
            return false;

        if(this.value != other.value)
            return false;

        return true;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("unit", unit)
                .append("value", value)
                .toString();
    }
}
