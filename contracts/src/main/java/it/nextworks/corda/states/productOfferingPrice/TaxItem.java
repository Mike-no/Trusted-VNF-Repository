package it.nextworks.corda.states.productOfferingPrice;

import net.corda.core.serialization.CordaSerializable;

@CordaSerializable
public class TaxItem {

    private final String taxCategory;
    private final float taxRate;
    private final Money taxAmount;

    /**
     * Constructor of the TaxItem class.
     * @param taxCategory    Tax category.
     * @param taxRate        Applied rate of teh tax.
     * @param taxAmount      Amount of tax expressed in the given currency.
     */
    public TaxItem(String taxCategory, float taxRate, Money taxAmount) {
        if(taxCategory == null || taxCategory.isEmpty() || taxCategory.trim().length() == 0)
            throw new IllegalArgumentException("The <taxCategory> cannot be null, empty or only composed by whitespace.");

        if(taxRate < 0)
            throw new IllegalArgumentException("The <taxRate> parameter cannot be negative.");

        if(taxAmount == null)
            throw new IllegalArgumentException("The <taxAmount> parameter cannot be null.");

        this.taxCategory    = taxCategory;
        this.taxRate        = taxRate;
        this.taxAmount      = taxAmount;
    }

    /* Getters */

    public String getTaxCategory() { return taxCategory; }

    public float getTaxRate() { return taxRate; }

    public Money getTaxAmount() { return taxAmount; }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof TaxItem))
            return false;

        TaxItem other = (TaxItem)obj;

        if(!(this.taxCategory.equals(other.taxCategory)))
            return false;

        if(this.taxRate != other.taxRate)
            return false;

        if(!(this.taxAmount.equals(other.taxAmount)))
            return false;

        return true;
    }
}
