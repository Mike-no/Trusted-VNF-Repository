package it.nextworks.corda.states.productOfferingPrice;

import net.corda.core.serialization.CordaSerializable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@CordaSerializable
public class TimePeriod {

    private final String startDateTime;
    private final String endDateTime;

    /**
     * Constructor of the TimePeriod class.
     * @param startDateTime start of the time period
     * @param endDateTime   end of the time period
     */
    public TimePeriod(String startDateTime, String endDateTime) {
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
}
