package it.nextworks.corda.states.productOfferingPrice;

import net.corda.core.serialization.CordaSerializable;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

@CordaSerializable
public class ProductOfferingPrice {

    private final String id;
    private final String href;
    private final String description;
    private final Boolean isBundle;
    private final String lastUpdate;
    private final String lifecycleStatus;
    private final String name;
    private final float percentage;
    private final String priceType;
    private final int recurringChargePeriodLength;
    private final String recurringChargePeriodType;
    private final String version;
    private final Money price;
    private final Quantity unitOfMeasure;
    private final TimePeriod validFor;

    private final List<TaxItem> tax = null;
    private final List<ProductOfferingTerm> productOfferingTerm = null;


    private static final String err = " cannot be null, empty or only composed by whitespace.";

    /**
     * Complete constructor of the ProductOfferingPrice class. Use the other constructor for a proper usage
     * of this class in a PkgOfferState environment.
     * @param id                          Unique id of this resource.
     * @param href                        Reference of the ProductOfferingPrice.
     * @param description                 Description of the productOfferingPrice.
     * @param isBundle                    A flag indicating id this ProductOfferingPrice is composite(bundle) or not.
     * @param lastUpdate                  The last update time of this ProductOfferingPrice.
     * @param lifecycleStatus             The lifecycle status of this ProductOfferingPrice.
     * @param name                        Name of the ProductOfferingPrice.
     * @param percentage                  Percentage to apply for ProductOfferPriceAlteration.
     * @param priceType                   A category that describes the price, such as recurring, discount, allowance,
     *                                    penalty, and so forth.
     * @param recurringChargePeriodLength The period of the recurring charge: 1, 2,...; it sets to zero if it is not
     *                                    applicable.
     * @param recurringChargePeriodType   The period to repeat the application of teh price; could be month, week, ...
     * @param version                     ProductOffering version.
     * @param price                       The amount of monet that characterizes the price.
     * @param unitOfMeasure               A number an unit representing how many of an ProductOffering is available at
     *                                    the offered price. Its meaning depends on the priceType. It could be a price,
     *                                    a rate, or a discount.
     * @param validFor                    The period for which the productOfferingPrice is valid.
     */
    public ProductOfferingPrice(String id,
                                String href,
                                String description,
                                Boolean isBundle,
                                String lastUpdate,
                                String lifecycleStatus,
                                String name,
                                float percentage,
                                String priceType,
                                int recurringChargePeriodLength,
                                String recurringChargePeriodType,
                                String version,
                                Money price,
                                Quantity unitOfMeasure,
                                TimePeriod validFor) {
        if(notWellFormatted(id))
            throw new IllegalArgumentException("The <id> parameter" + err);

        try {
            new URL(href);
        } catch (MalformedURLException mue) {
            throw new IllegalArgumentException("The <href> parameter is not a link.");
        }

        if(notWellFormatted(description))
            throw new IllegalArgumentException("The <description> parameter" + err);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'0Z'");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(lastUpdate.trim());
        } catch(ParseException pe) {
            throw new IllegalArgumentException("The <lastUpdate> parameter is not in the correct Date format.");
        }

        if(notWellFormatted(lifecycleStatus))
            throw new IllegalArgumentException("The <lifecycleStatus> parameter" + err);

        if(notWellFormatted(name))
            throw new IllegalArgumentException("The <name> parameter" + err);

        if(percentage < 0)
            throw new IllegalArgumentException("The <percentage> parameter cannot be negative.");

        if(notWellFormatted(priceType))
            throw new IllegalArgumentException("The <priceType> parameter" + err);

        if(recurringChargePeriodLength < 0)
            throw new IllegalArgumentException("The <recurringChargePeriodLength> parameter cannot be negative.");

        if(notWellFormatted(recurringChargePeriodType))
            throw new IllegalArgumentException("The <recurringChargePeriodType> parameter" + err);

        if(notWellFormatted(version))
            throw new IllegalArgumentException("The <version> parameter" + err);

        if(price == null)
            throw new IllegalArgumentException("The <price> parameter cannot be null.");

        if(unitOfMeasure == null)
            throw new IllegalArgumentException("The <unitOfMeasure> parameter cannot be null.");

        if(validFor == null)
            throw new IllegalArgumentException("The <validFor> parameter cannot be null.");

        this.id                          = id;
        this.href                        = href;
        this.description                 = description;
        this.isBundle                    = isBundle;
        this.lastUpdate                  = lastUpdate;
        this.lifecycleStatus             = lifecycleStatus;
        this.name                        = name;
        this.percentage                  = percentage;
        this.priceType                   = priceType;
        this.recurringChargePeriodLength = recurringChargePeriodLength;
        this.recurringChargePeriodType   = recurringChargePeriodType;
        this.version                     = version;
        this.price                       = price;
        this.unitOfMeasure               = unitOfMeasure;
        this.validFor                    = validFor;
    }

    /* Getters */

    public String getId() { return id; }

    public String getHref() { return href; }

    public String getDescription() {  return description; }

    public Boolean getIsBundle() { return isBundle; }

    public String getLastUpdate() { return lastUpdate; }

    public String getLifecycleStatus() { return lifecycleStatus; }

    public String getName() { return name; }

    public float getPercentage() { return percentage; }

    public String getPriceType() { return priceType; }

    public int getRecurringChargePeriodLength() { return recurringChargePeriodLength; }

    public String getRecurringChargePeriodType() { return recurringChargePeriodType; }

    public String getVersion() { return version; }

    public Money getPrice() { return price; }

    public Quantity getUnitOfMeasure() { return unitOfMeasure; }

    public TimePeriod getValidFor() { return validFor; }

    private boolean notWellFormatted(String str){
        return str == null || str.isEmpty() || str.trim().length() == 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof ProductOfferingPrice))
            return false;

        ProductOfferingPrice other = (ProductOfferingPrice)obj;

        if(!(this.id.equals(other.id)))
            return false;

        if(!(this.href.equals(other.href)))
            return false;

        if(!(this.description.equals(other.description)))
            return false;

        if(this.isBundle != other.isBundle)
            return false;

        if(!(this.lastUpdate.equals(other.lastUpdate)))
            return false;

        if(!(this.lifecycleStatus.equals(other.lifecycleStatus)))
            return false;

        if(!(this.name.equals(other.name)))
            return false;

        if(this.percentage != other.percentage)
            return false;

        if(!(this.priceType.equals(other.priceType)))
            return false;

        if(this.recurringChargePeriodLength != other.recurringChargePeriodLength)
            return false;

        if(!(this.recurringChargePeriodType.equals(other.recurringChargePeriodType)))
            return false;

        if(!(this.version.equals(other.version)))
            return false;

        if(!(this.price.equals(other.price)))
            return false;

        if(!(this.unitOfMeasure.equals(other.unitOfMeasure)))
            return false;

        if(!(this.validFor.equals(other.validFor)))
            return false;

        return true;
    }
}
