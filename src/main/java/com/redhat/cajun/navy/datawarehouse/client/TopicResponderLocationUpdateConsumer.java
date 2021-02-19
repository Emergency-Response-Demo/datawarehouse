package com.redhat.cajun.navy.datawarehouse.client;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.DatawarehouseService;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.ResponderLocationUpdate;
import com.redhat.cajun.navy.datawarehouse.util.Constants;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.ce.IncomingCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    DatawarehouseService dService;

    @Inject
    @ConfigProperty(name = LOG_RESPONDER_LOCATION_UPDATE_COMSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-responder-location-update")
    @Blocking // Ensure execution occurs on a worker thread rather than on the event loop thread (which whould never be blocked)
    @Acknowledgment(Acknowledgment.Strategy.PRE_PROCESSING)  // Ack message prior to message processing
    public CompletionStage<Void> process(IncomingKafkaRecord<String, String> message) {
        Optional<IncomingCloudEventMetadata> metadata = message.getMetadata(IncomingCloudEventMetadata.class);
        if (metadata.isEmpty()) {
            logger.warn("Incoming message is not a CloudEvent");
            return CompletableFuture.completedFuture(null);
        }
        IncomingCloudEventMetadata<String> cloudEventMetadata = metadata.get();
        String dataContentType = cloudEventMetadata.getDataContentType().orElse("");
        if (!dataContentType.equalsIgnoreCase("application/json")) {
            logger.warn("CloudEvent data content type is not specified or not 'application/json'. Message is ignored");
            return CompletableFuture.completedFuture(null);
        }
        if (StringUtils.isEmpty(message.getPayload())) {
            logger.warn("process() empty message body");
            return CompletableFuture.completedFuture(null);
        }
        String type = cloudEventMetadata.getType();
        if (!("ResponderLocationUpdatedEvent".equals(type))) {
            logger.debug("CloudEvent with type '" + type + "' is ignored");
            return CompletableFuture.completedFuture(null);
        }
        if (this.log) {
            logger.info("process() topic-responder-location-update = " + message.getPayload());
        }
        ResponderLocationUpdate rlObj = Json.decodeValue(message.getPayload(), ResponderLocationUpdate.class);
        if (rlObj.getStatus().equals(ResponderLocationUpdate.Statuses.PICKEDUP.name())) {
            String incidentId = rlObj.getIncidentId();

            // Set pickup point in Mission Report.
            // The equivalent MissionCompletedEvent.steps (retrieved from MapBox) may not
            // correspond to actual steps in responderLocationHistory
            try {
                updateCache(rlObj.getIncidentId(), rlObj);
            } catch(HotRodClientException x) {
                logger.error(rlObj.getIncidentId()+" : "+Constants.HOTROD_CLIENT_EXCEPTION+ " : processing ResponderLocationUpdateEvent() .... will try again");
                try {
                    Thread.sleep(250);
                    updateCache(rlObj.getIncidentId(), rlObj);
                }catch(Exception y){
                    logger.error(rlObj.getIncidentId()+" Error processing ResponderLocationUpdateEvent() after second attempt !!!");
                    y.printStackTrace();
                }
            }catch(Throwable x) {
                logger.error(incidentId+" Error processing location-update");
                x.printStackTrace();
            }
        }
        return CompletableFuture.completedFuture(null);
    }
    
    private void updateCache(String id, ResponderLocationUpdate rlObj) throws Exception {
       
        MetadataValue<MissionReport> mValue = dService.getMissionReportCache().getWithMetadata(id);
        if(mValue != null) {
            MissionReport mReport = mValue.getValue();
            mReport.setPickupLat(rlObj.getLat());
            mReport.setPickupLong(rlObj.getLon());
            dService.getMissionReportCache().replaceWithVersion(rlObj.getIncidentId(), mReport, mValue.getVersion());
            logger.info(mReport.getIncidentId()+" : Just updated with pickupLat = "+rlObj.getLat());
        }else {
            logger.error(id+" "+Constants.NO_REPORT_FOUND_EXCEPTION);
        }
    }

}
