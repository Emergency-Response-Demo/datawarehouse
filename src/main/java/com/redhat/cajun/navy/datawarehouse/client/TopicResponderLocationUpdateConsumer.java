package com.redhat.cajun.navy.datawarehouse.client;

import com.redhat.cajun.navy.datawarehouse.util.Constants;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.ResponderLocationUpdate;

import javax.transaction.UserTransaction;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import io.quarkus.infinispan.client.Remote;
import io.smallrye.reactive.messaging.annotations.Blocking;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.vertx.core.json.Json;

/*
 *   Purpose:
 *     Consumes a location-update message (with status of PICKEDUP) from topic-responder-location-update kafka topic :
 * 
 *    Sample message as follows: 
 *        {"responderId":"8","missionId":"c6dfad30-8481-4500-803a-f4728cbfa70c","incidentId":"d01ab222-1561-427c-a185-72698b83f5a0","status":"PICKEDUP","lat":34.1853,"lon":-77.8609,"human":false,"continue":true}
 */
@ApplicationScoped
public class TopicResponderLocationUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicResponderLocationUpdateConsumer");
    private static final String LOG_RESPONDER_LOCATION_UPDATE_COMSUMER = "er.demo.LOG_RESPONDER_LOCATION_UPDATE_COMSUMER";
    private boolean log = true;

    @Inject
    @Remote(Constants.INCIDENT_MAP)
    RemoteCache<String, MissionReport> mMap;

    @Inject
    @ConfigProperty(name = LOG_RESPONDER_LOCATION_UPDATE_COMSUMER, defaultValue = "False")
    String logRawEvents;

    @Inject UserTransaction transaction;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-responder-location-update")
    @Blocking // Ensure execution occurs on a worker thread rather than on the event loop thread (which whould never be blocked)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)  // Ack message prior to message processing
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
            String incidentId = rlObj.getIncidentId();

            // Set pickup point in Mission Report.
            // The equivalent MissionCompletedEvent.steps (retrieved from MapBox) may not
            // correspond to actual steps in responderLocationHistory
            try {
                transaction.begin();
                MetadataValue<MissionReport> mValue = mMap.getWithMetadata(incidentId);
                if(mValue != null) {
                    MissionReport mReport = mValue.getValue();
                    mReport.setPickupLat(rlObj.getLat());
                    mReport.setPickupLong(rlObj.getLon());
                    mMap.replaceWithVersion(rlObj.getIncidentId(), mReport, mValue.getVersion());
                    logger.info(mReport.getIncidentId()+" : Just updated with pickupLat = "+rlObj.getLat());
                }else {
                    logger.error(incidentId+" "+Constants.NO_REPORT_FOUND_EXCEPTION);
                }
                transaction.commit();
            }catch(Throwable x) {
                logger.error(incidentId+" Error processing location-update");
                x.printStackTrace();
            }
            
        }
    }

}
