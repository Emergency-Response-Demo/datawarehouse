package com.redhat.cajun.navy.datawarehouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.redhat.cajun.navy.datawarehouse.util.DoubleContextualSerializer;
import com.redhat.cajun.navy.datawarehouse.util.Precision;
import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponderLocationHistory {

    private long timestamp;
    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lon;

    public ResponderLocationHistory() {

    }

    @ProtoFactory
    public ResponderLocationHistory(long timestamp, double lat, double lon) {
        this.timestamp = timestamp;
        this.lat = lat;
        this.lon = lon;

    }

    @ProtoField(number = 1, defaultValue="0.0")
    public double getLat() {
        return lat;
    }

    public void setLat(double input) {
        this.lat = input;
    }

    @ProtoField(number = 2, defaultValue="0.0")
    public double getLon() {
        return lon;
    }

    public void setLon(double input) {
        this.lon = input;
    }

    @ProtoField(number = 3, defaultValue="0")
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long input) {
        this.timestamp = input;
    }

	@Override
	public String toString() {
		return "[lat=" + lat + ", lon=" + lon + ", timestamp=" + timestamp + "]";
	}

    

}
