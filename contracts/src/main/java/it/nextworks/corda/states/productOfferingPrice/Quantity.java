package it.nextworks.corda.states.productOfferingPrice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.corda.core.serialization.CordaSerializable;
import org.apache.commons.lang3.builder.ToStringBuilder;

@CordaSerializable
public class Quantity {

    @JsonProperty(value = "amount") private final float amount;
    @JsonProperty(value = "unit") private final String unit;

    /**
     * Constructor of the Quantity class.
     * @param amount Numeric value in a given unit
     * @param unit   unit
     */
    @JsonCreator
    public Quantity(@JsonProperty(value = "amount") float amount, @JsonProperty(value = "unit") String unit) {
        if(amount < 0)
            throw new IllegalArgumentException("The <amount> parameter cannot be negative.");

        if(unit == null || unit.isEmpty() || unit.trim().length() == 0)
            throw new IllegalArgumentException(
                    "The <unit> parameter cannot be null, empty or only composed by whitespace.");

        this.amount = amount;
        this.unit = unit;
    }

    /* Getters */

    public float getAmount() { return amount; }

    public String getUnit() { return unit; }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof Quantity))
            return false;

        Quantity other = (Quantity)obj;

        if(this.amount != other.amount)
            return false;

        if(!(this.unit.equals(other.unit)))
            return false;

        return true;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("amount", amount)
                .append("unit", unit)
                .toString();
    }
}
