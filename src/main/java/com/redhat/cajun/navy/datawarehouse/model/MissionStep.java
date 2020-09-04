package com.redhat.cajun.navy.datawarehouse.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.mapbox.geojson.Point;
import com.redhat.cajun.navy.datawarehouse.util.DoubleContextualSerializer;
import com.redhat.cajun.navy.datawarehouse.util.Precision;

import org.infinispan.protostream.annotations.ProtoFactory;
import org.infinispan.protostream.annotations.ProtoField;

import io.vertx.core.json.Json;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MissionStep {

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lat;

    @JsonSerialize(using = DoubleContextualSerializer.class)
    @Precision(precision = 4)
    private double lon;

    private boolean isWayPoint = false;

    private boolean isDestination = false;

    public MissionStep() {
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

    @ProtoField(number = 3, defaultValue="false")
    public boolean getIsWayPoint() {
        return isWayPoint;
    }

    public void setIsWayPoint(boolean wayPoint) {
        isWayPoint = wayPoint;
    }

    @ProtoField(number = 4, defaultValue="false")
    public boolean getIsDestination() {
        return isDestination;
    }

    public void setDestination(boolean destination) {
        isDestination = destination;
    }

    public MissionStep(Point p) {
        lat = p.latitude();
        lon = p.longitude();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MissionStep step = (MissionStep) o;

        return Double.compare(step.lat, lat) == 0 && Double.compare(step.lon, lon) == 0
                && step.isDestination == isDestination && step.isWayPoint == isWayPoint;
    }

    public String toJson() {
        return Json.encode(this);
    }

    @Override
    public String toString() {
        return toJson();
    }

    @ProtoFactory
	public MissionStep(double lat, double lon, boolean isWayPoint, boolean isDestination) {
		this.lat = lat;
		this.lon = lon;
		this.isWayPoint = isWayPoint;
		this.isDestination = isDestination;
	}
}
