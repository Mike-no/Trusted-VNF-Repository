package it.nextworks.corda.states.productOfferingPrice;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class ProductOfferingTerm {

    private final String description;
    private final String name;
    private final Quantity duration;
    private final TimePeriod validFor;

    private static final String err = " cannot be null, empty or only composed by whitespace.";

    /**
     * Constructor of the ProductOfferingTerm class.
     * @param description    Description of the productOfferingTerm.
     * @param name           Name of the productOfferingTerm.
     * @param duration       Duration of the productOfferingTerm.
     * @param validFor       The period for which the productOfferingTerm is valid.
     */
    public ProductOfferingTerm(String description, String name, Quantity duration, TimePeriod validFor) {
        if(notWellFormatted(description))
            throw new IllegalArgumentException("The <name> parameter" + err);

        if(notWellFormatted(name))
            throw new IllegalArgumentException("The <description> parameter" + err);

        if(duration == null)
            throw new IllegalArgumentException("The <duration> parameter cannot be null.");

        if(validFor == null)
            throw new IllegalArgumentException("The <validFor> parameter cannot be null.");

        this.description    = description;
        this.name           = name;
        this.duration       = duration;
        this.validFor       = validFor;
    }

    /* Getters */

    public String getDescription() { return description; }

    public String getName() { return name; }

    public Quantity getDuration() { return duration; }

    public TimePeriod getValidFor() { return validFor; }

    private boolean notWellFormatted(String str){
        return str == null || str.isEmpty() || str.trim().length() == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof ProductOfferingTerm))
            return false;

        ProductOfferingTerm other = (ProductOfferingTerm)obj;

        if(!(this.description.equals(other.description)))
            return false;

        if(!(this.name.equals(other.name)))
            return false;

        if(!(this.duration.equals(other.duration)))
            return false;

        if(!(this.validFor.equals(other.validFor)))
            return false;

        return true;
    }
}
