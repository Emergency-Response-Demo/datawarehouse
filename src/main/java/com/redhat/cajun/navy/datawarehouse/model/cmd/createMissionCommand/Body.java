package com.redhat.cajun.navy.datawarehouse.model.cmd.createMissionCommand;

import java.math.BigDecimal;

public class Body {

    private String incidentId;
    private String responderId;
    private BigDecimal responderStartLat;
    private BigDecimal responderStartLong;
    private BigDecimal incidentLat;
    private BigDecimal incidentLong;
    private BigDecimal destinationLat;
    private BigDecimal destinationLong;
    private String processId;

    public String getIncidentId() {
        return incidentId;
    }

    public void setIncidentId(String incidentId) {
        this.incidentId = incidentId;
    }

    public String getResponderId() {
        return responderId;
    }

    public void setResponderId(String responderId) {
        this.responderId = responderId;
    }

    public BigDecimal getResponderStartLat() {
        return responderStartLat;
    }

    public void setResponderStartLat(BigDecimal responderStartLat) {
        this.responderStartLat = responderStartLat;
    }

    public BigDecimal getResponderStartLong() {
        return responderStartLong;
    }

    public void setResponderStartLong(BigDecimal responderStartLong) {
        this.responderStartLong = responderStartLong;
    }

    public BigDecimal getIncidentLat() {
        return incidentLat;
    }

    public void setIncidentLat(BigDecimal incidentLat) {
        this.incidentLat = incidentLat;
    }

    public BigDecimal getIncidentLong() {
        return incidentLong;
    }

    public void setIncidentLong(BigDecimal incidentLong) {
        this.incidentLong = incidentLong;
    }

    public BigDecimal getDestinationLat() {
        return destinationLat;
    }

    public void setDestinationLat(BigDecimal destinationLat) {
        this.destinationLat = destinationLat;
    }

    public BigDecimal getDestinationLong() {
        return destinationLong;
    }

    public void setDestinationLong(BigDecimal destinationLong) {
        this.destinationLong = destinationLong;
    }

    public String getProcessId() {
        return processId;
    }

    public void setProcessId(String processId) {
        this.processId = processId;
    }

}