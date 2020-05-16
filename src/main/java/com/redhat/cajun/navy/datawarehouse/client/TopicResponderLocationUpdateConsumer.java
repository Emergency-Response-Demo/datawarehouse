package com.redhat.cajun.navy.datawarehouse.client;

import com.redhat.cajun.navy.datawarehouse.util.Constants;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.ResponderLocationUpdate;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.json.Json;

/*
 *   Consumes the following message type on the topic-responder-location-update kafka topic:
 * 
 *    {"responderId":"8","missionId":"c6dfad30-8481-4500-803a-f4728cbfa70c","incidentId":"d01ab222-1561-427c-a185-72698b83f5a0","status":"MOVING","lat":34.1853,"lon":-77.8609,"human":false,"continue":true}
 */
@ApplicationScoped
public class TopicResponderLocationUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicResponderLocationUpdateConsumer");
    private static final String LOG_RESPONDER_LOCATION_UPDATE_COMSUMER = "er.demo.LOG_RESPONDER_LOCATION_UPDATE_COMSUMER";
    private boolean log = true;

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    @Inject
    @ConfigProperty(name = LOG_RESPONDER_LOCATION_UPDATE_COMSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-responder-location-update")
    public void process(String topicCommand) {
        if (StringUtils.isEmpty(topicCommand)) {
            logger.warn("process() empty message body");
            return;
        }
        if (this.log) {
            logger.info("process() topic-responder-location-update = " + topicCommand);
        }
        ResponderLocationUpdate rlObj = Json.decodeValue(topicCommand, ResponderLocationUpdate.class);
        if (rlObj.getStatus().equals(ResponderLocationUpdate.Statuses.PICKEDUP.name())) {

            // Set pickup point in Mission Report.
            // The equivalent MissionCompletedEvent.steps (retrieved from MapBox) may not
            // correspond to actual steps in responderLocationHistory
            LocalMap<String, MissionReport> mMap = vertx.getDelegate().sharedData().getLocalMap(Constants.MISSION_MAP);
            MissionReport mReport = mMap.get(rlObj.getMissionId());
            if(mReport != null) {
                mReport.setPickupLat(rlObj.getLat());
                mReport.setPickupLong(rlObj.getLon());
            }else {
                logger.error(Constants.NO_REPORT_FOUND_EXCEPTION+" : No MissionReport found in cache for reportId = "+rlObj.getMissionId());
            }

        }
    }

}