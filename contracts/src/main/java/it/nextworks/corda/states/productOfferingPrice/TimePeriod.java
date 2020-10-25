package it.nextworks.corda.states.productOfferingPrice;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.corda.core.serialization.CordaSerializable;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@CordaSerializable
public class TimePeriod {

    @JsonProperty(value = "startDateTime") private final String startDateTime;
    @JsonProperty(value = "endDateTime") private final String endDateTime;

    /**
     * Constructor of the TimePeriod class.
     * @param startDateTime start of the time period
     * @param endDateTime   end of the time period
     */
    @JsonCreator
    public TimePeriod(@JsonProperty(value = "startDateTime") String startDateTime,
                      @JsonProperty(value = "endDateTime") String endDateTime) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.'0Z'");
        dateFormat.setLenient(false);

        Date start;
        try {
            start = dateFormat.parse(startDateTime.trim());
        } catch(ParseException pe) {
            throw new IllegalArgumentException("The <startDateTime> parameter is not in the correct Date format.");
        }

        Date end;
        try {
            end = dateFormat.parse(endDateTime.trim());
        } catch(ParseException pe) {
            throw new IllegalArgumentException("The <endDateTime> parameter is not in the correct Date format.");
        }

        if(!end.after(start))
            throw new IllegalArgumentException("The <endDateTime> parameter is not after the <startDateTime> parameter");

        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    /* Getters */

    public String getStartDateTime() { return startDateTime; }

    public String getEndDateTime() { return endDateTime; }

    @Override
    public boolean equals(Object obj) {
        if(obj == null)
            return false;

        if(!(obj instanceof TimePeriod))
            return false;

        TimePeriod other = (TimePeriod)obj;

        if(!(this.startDateTime.equals(other.startDateTime)))
            return false;

        if(!(this.endDateTime.equals(other.endDateTime)))
            return false;

        return true;
    }

    public String toString() {
        return new ToStringBuilder(this)
                .append("startDateTime", startDateTime)
                .append("endDateTime", endDateTime)
                .toString();
    }
}
