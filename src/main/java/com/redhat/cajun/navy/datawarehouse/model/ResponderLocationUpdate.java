package com.redhat.cajun.navy.datawarehouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.redhat.cajun.navy.datawarehouse.util.DoubleContextualSerializer;
import com.redhat.cajun.navy.datawarehouse.util.Precision;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponderLocationUpdate {

    public static enum Statuses {
        MOVING, PICKEDUP, DROPPED
    };

    private String responderId;
    private String missionId;
    private String incidentId;
    private String status;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lon;
    private Boolean human;

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String responderId) {
        this.responderId = responderId;
    }

    public String getMissionId() {
        return missionId;
    }

    public void setMissionId(String missionId) {
        this.missionId = missionId;
    }

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidientId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Boolean getHuman() {
        return human;
    }

    public void setHuman(Boolean human) {
        this.human = human;
    }

}