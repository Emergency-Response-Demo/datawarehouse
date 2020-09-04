package com.redhat.cajun.navy.datawarehouse.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.redhat.cajun.navy.datawarehouse.util.CalculationException;
import com.redhat.cajun.navy.datawarehouse.util.DoubleContextualSerializer;
import com.redhat.cajun.navy.datawarehouse.util.Precision;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import io.vertx.core.json.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionReport implements io.vertx.core.shareddata.Shareable {

    @ProtoField(number = 1)
    public String id;

    @ProtoField(number = 2)
    public String status;

    @ProtoField(number = 3)
    public String incidentId;

    @ProtoField(number = 4)
    public String responderId;

    @ProtoField(number = 5)
    public String responderFullName;

    @ProtoField(number = 6, defaultValue="false")
    public boolean responderHasMedicalKit = false;

    @ProtoField(number = 7, defaultValue="0")
    public int numberRescued;

    @ProtoField(number = 8, defaultValue="0.0")
    public double responderDistancePickup;

    @ProtoField(number = 9, defaultValue="0.0")
    public double responderDistanceDropoff;

    @ProtoField(number = 10, defaultValue="0.0")
    public double responderDistanceTotal;

    @ProtoField(number = 11, defaultValue="0")
    public long responseTimeSecondsPickup;

    @ProtoField(number = 12, defaultValue="0")
    public long responseTimeSecondsDropoff;

    @ProtoField(number = 13, defaultValue="0")
    public long responseTimeSecondsTotal;

    @ProtoField(number = 14)
    public String processInstanceId;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 15, defaultValue="0.0")
    public double pickupLat = 0.0;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 16, defaultValue="0.0")
    public double pickupLong = 0.0;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 17, defaultValue="0.0")
    public double responderStartLat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 18, defaultValue="0.0")
    public double responderStartLong;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 19, defaultValue="0.0")
    public double incidentLat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 20, defaultValue="0.0")
    public double incidentLong;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 21, defaultValue="0.0")
    public double destinationLat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    @ProtoField(number = 22, defaultValue="0.0")
    public double destinationLong;

    @ProtoField(number = 23, collectionImplementation = ArrayList.class)
    public List<ResponderLocationHistory> responderLocationHistory;

    @ProtoField(number = 24, collectionImplementation = ArrayList.class)
    public List<MissionStep> steps = null;

    public MissionReport() {
    }

    @ProtoFactory
	public MissionReport(String id, String status, String incidentId, String responderId, String responderFullName,
			boolean responderHasMedicalKit, int numberRescued, double responderDistancePickup,
			double responderDistanceDropoff, double responderDistanceTotal, long responseTimeSecondsPickup,
			long responseTimeSecondsDropoff, long responseTimeSecondsTotal, String processInstanceId, double pickupLat,
			double pickupLong, double responderStartLat, double responderStartLong, double incidentLat,
			double incidentLong, double destinationLat, double destinationLong,
			List<ResponderLocationHistory> responderLocationHistory, List<MissionStep> steps) {
		this.id = id;
		this.status = status;
		this.incidentId = incidentId;
		this.responderId = responderId;
		this.responderFullName = responderFullName;
		this.responderHasMedicalKit = responderHasMedicalKit;
		this.numberRescued = numberRescued;
		this.responderDistancePickup = responderDistancePickup;
		this.responderDistanceDropoff = responderDistanceDropoff;
		this.responderDistanceTotal = responderDistanceTotal;
		this.responseTimeSecondsPickup = responseTimeSecondsPickup;
		this.responseTimeSecondsDropoff = responseTimeSecondsDropoff;
		this.responseTimeSecondsTotal = responseTimeSecondsTotal;
		this.processInstanceId = processInstanceId;
		this.pickupLat = pickupLat;
		this.pickupLong = pickupLong;
		this.responderStartLat = responderStartLat;
		this.responderStartLong = responderStartLong;
		this.incidentLat = incidentLat;
		this.incidentLong = incidentLong;
		this.destinationLat = destinationLat;
		this.destinationLong = destinationLong;
		this.responderLocationHistory = responderLocationHistory;
		this.steps = steps;
    }

    public String getId() {
        return id;
    }

    public void setId(String input) {
        this.id = input;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String input) {
        this.incidentId = input;
    }

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String input) {
        this.responderId = input;
    }

    public double getResponderStartLat() {
        return responderStartLat;
    }

    public void setResponderStartLat(double input) {
        this.responderStartLat = input;
    }

    public double getResponderStartLong() {
        return responderStartLong;
    }

    public void setResponderStartLong(double input) {
        this.responderStartLong = input;
    }

    public double getIncidentLat() {
        return incidentLat;
    }

    public void setIncidentLat(double input) {
        this.incidentLat = input;
    }

    public double getIncidentLong() {
        return incidentLong;
    }

    public void setIncidentLong(double input) {
        this.incidentLong = input;
    }

    public double getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(double input) {
        this.destinationLat = input;
    }

    public double getDestinationLong() {
        return destinationLong;
    }

    public void setDestinationLong(double input) {
        this.destinationLong = input;
    }

    
    public List<ResponderLocationHistory> getResponderLocationHistory() {
        return responderLocationHistory;
    }

    public void setResponderLocationHistory(List<ResponderLocationHistory> input) {
        this.responderLocationHistory = input;
    }

    public void addResponderLocationHistory(ResponderLocationHistory history) {
        responderLocationHistory.add(history);
        setResponderStartLat(history.getLat());
        setResponderStartLong(history.getLon());

    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String input) {
        this.status = input;
    }

    public String getResponderFullName() {
        return responderFullName;
    }

    public void setResponderFullName(String responderFullName) {
        this.responderFullName = responderFullName;
    }

    public double getResponderDistancePickup() {
        return responderDistancePickup;
    }

    public void setResponderDistancePickup(double responderDistancePickup) {
        this.responderDistancePickup = responderDistancePickup;
    }

    public double getResponderDistanceDropoff() {
        return responderDistanceDropoff;
    }

    public void setResponderDistanceDropoff(double responderDistanceDropoff) {
        this.responderDistanceDropoff = responderDistanceDropoff;
    }

    public double getResponderDistanceTotal() {
        return responderDistanceTotal;
    }

    public void setResponderDistanceTotal(double responderDistanceTotal) {
        this.responderDistanceTotal = responderDistanceTotal;
    }

    public boolean isResponderHasMedicalKit() {
        return responderHasMedicalKit;
    }

    public void setResponderHasMedicalKit(boolean responderHasMedicalKit) {
        this.responderHasMedicalKit = responderHasMedicalKit;
    }

    public int getNumberRescued() {
        return numberRescued;
    }

    public void setNumberRescued(int numberRescued) {
        this.numberRescued = numberRescued;
    }

    public long getResponseTimeSecondsPickup() {
        return responseTimeSecondsPickup;
    }

    public void setResponseTimeSecondsPickup(long responseTimeSecondsPickup) {
        this.responseTimeSecondsPickup = responseTimeSecondsPickup;
    }

    public long getResponseTimeSecondsDropoff() {
        return responseTimeSecondsDropoff;
    }

    public void setResponseTimeSecondsDropoff(long responseTimeSecondsDropoff) {
        this.responseTimeSecondsDropoff = responseTimeSecondsDropoff;
    }

    public long getResponseTimeSecondsTotal() {
        return responseTimeSecondsTotal;
    }

    public void setResponseTimeSecondsTotal(long responseTimeSecondsTotal) {
        this.responseTimeSecondsTotal = responseTimeSecondsTotal;
    }

    public double getPickupLog() {
        return pickupLat;
    }

    public void setPickupLat(double lat) {
        pickupLat = lat;
    }

    public double getPickupLong() {
        return pickupLong;
    }

    public void setPickupLong(double lon) {
        pickupLong = lon;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public void setSteps(List<MissionStep> steps) {
        this.steps = steps;
    }

    public void addMissionStep(MissionStep step) {
        if (this.steps != null && step != null) {
            steps.add(step);
        } else
            throw new IllegalArgumentException("Null value not acceptable");
    }

    public List<MissionStep> getSteps() {
        return steps;
    }

    public void calculateDistancesAndTimes() throws CalculationException {
        if (pickupLat == 0.0) {
            throw new CalculationException(this.incidentId+" : Cant calculate; pickupLat == 0.0");
        }

        ResponderLocationHistory rLocH0 = null;
        boolean pickedup = false;
        for (ResponderLocationHistory rLocH1 : responderLocationHistory) {
            if (rLocH0 == null) {
                rLocH0 = rLocH1;
                continue;
            }
            double distanceMeters = distanceMeters(rLocH0.getLat(), rLocH0.getLon(), rLocH1.getLat(), rLocH1.getLon());
            long durationSec = (rLocH1.getTimestamp() - rLocH0.getTimestamp()) / 1000;

            if (!pickedup) {
                responderDistancePickup = responderDistancePickup + distanceMeters;
                responseTimeSecondsPickup = responseTimeSecondsPickup + durationSec;
                if ((pickupLat == rLocH1.getLat()) && (pickupLong == rLocH1.getLon())) {
                    pickedup = true;
                }
            } else {
                responderDistanceDropoff = responderDistanceDropoff + distanceMeters;
                responseTimeSecondsDropoff = responseTimeSecondsDropoff + durationSec;
            }
            rLocH0 = rLocH1;
        }

        if(!pickedup) {
            StringBuilder sBuilder = new StringBuilder(this.incidentId+" : pickup location doesn't match history : "+pickupLat+" : "+pickupLong+"\n");
            sBuilder.append(dumpResponderHistoryList());
            throw new CalculationException(sBuilder.toString());
        }

        this.responderDistanceTotal = this.responderDistancePickup + this.responderDistanceDropoff;
        this.responseTimeSecondsTotal = this.responseTimeSecondsPickup + this.responseTimeSecondsDropoff;

    }

    private static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        if ((lat1 == lat2) && (lon1 == lon2)) {
            return 0;
        } else {

            double theta = lon1 - lon2;
            double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                    + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
            dist = Math.acos(dist);
            dist = Math.toDegrees(dist);
            dist = dist * 60 * 1.1515; // miles
            dist = dist * 1.609344 * 1000; // meters
            return (dist);
        }
    }

    public String toJson() {
        return Json.encode(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MissionReport mission = (MissionReport) o;
        return Objects.equals(responderId, mission.responderId) && Objects.equals(incidentId, mission.incidentId);
    }

    @JsonIgnore
    public String getKey() {
        return this.incidentId + this.responderId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(responderId + incidentId);
    }

    public String dumpResponderHistoryList() {
        StringBuilder sBuilder = new StringBuilder();
        int x = 0;
        for(ResponderLocationHistory rlh : this.responderLocationHistory){
            if(x != 0)
                sBuilder.append(" , ");

            sBuilder.append(rlh.toString());
            x++;
        }
        return sBuilder.toString();
    }

}
