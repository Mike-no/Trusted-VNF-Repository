package it.nextworks.corda.states.productOfferingPrice;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class Quantity {

    private final float amount;
    private final String unit;

    /**
     * Constructor of the Quantity class.
     * @param amount Numeric value in a given unit
     * @param unit   unit
     */
    public Quantity(float amount, String unit) {
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
}
