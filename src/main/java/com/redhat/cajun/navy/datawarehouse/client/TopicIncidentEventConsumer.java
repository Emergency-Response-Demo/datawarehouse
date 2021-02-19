package com.redhat.cajun.navy.datawarehouse.client;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.DatawarehouseService;
import com.redhat.cajun.navy.datawarehouse.model.Incident;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.cmd.incident.IncidentCommand;
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

/*  Purpose:
 *    Consume the IncidentUpdatedEvent (produced by incident-service) with a status of PICKEDUP from topic-incident-event Kafka topic
 *    This message contains the actual number of people that were picked up
*/
@ApplicationScoped
public class TopicIncidentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicIncidentEventConsumer");
    private static final String LOG_INCIDENT_EVENT_CONSUMER = "er.demo.LOG_INCIDENT_EVENT_COMSUMER";
    private boolean log = true;

    @Inject
    DatawarehouseService dService;

    @Inject
    @ConfigProperty(name = LOG_INCIDENT_EVENT_CONSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-incident-event")
    @Blocking // Ensure execution occurs on a worker thread rather than on the event loop thread (which would never be blocked)
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
        String type = cloudEventMetadata.getType();
        if (!(IncidentCommand.MessageTypes.IncidentUpdatedEvent.name().equals(type))) {
            logger.debug("CloudEvent with type '" + type + "' is ignored");
            return CompletableFuture.completedFuture(null);
        }
        if (StringUtils.isEmpty(message.getPayload())) {
            logger.warn("process() empty message body");
            return CompletableFuture.completedFuture(null);
        }
        if (this.log) {
            logger.info("process() topicIncidentEvent = " + message.getPayload());
        }

        Incident iObj = Json.decodeValue(message.getPayload(), Incident.class);
        if (Incident.Statuses.PICKEDUP.name().equals(iObj.getStatus())) {

            try {
                // If event = PICKEDUP, then update MissionReport with numberRescued
                updateCache(cloudEventMetadata.getId(), iObj);
            } catch(HotRodClientException x) {
                logger.error(cloudEventMetadata.getId()+" : "+Constants.HOTROD_CLIENT_EXCEPTION+ " : processing IncidentUpdatedEvent() .... will try again");
                try {
                    Thread.sleep(250);
                    updateCache(cloudEventMetadata.getId(), iObj);
                }catch(Exception y){
                    logger.error(cloudEventMetadata.getId()+" Error processing IncidentUpdatedEvent() after second attempt !!!");
                    y.printStackTrace();
                }
            } catch(Exception x) {
                logger.error(cloudEventMetadata.getId()+" Error processing IncidentUpdatedEvent()");
                x.printStackTrace();
            }
        }
        return CompletableFuture.completedFuture(null);
    }
    
    private void updateCache(String id, Incident iObj) throws Exception {

        MetadataValue<MissionReport> mValue = dService.getMissionReportCache().getWithMetadata(iObj.getId());
        if(mValue != null) {
            MissionReport mReport = mValue.getValue();
            mReport.setNumberRescued(iObj.getNumberOfPeople());
            dService.getMissionReportCache().replaceWithVersion(iObj.getId(), mReport, mValue.getVersion());
            logger.info(mReport.getIncidentId()+" : updated with following # of rescued people: "+iObj.getNumberOfPeople());
        }else {
            logger.error(id+" "+Constants.NO_REPORT_FOUND_EXCEPTION);
        }
        
    }

}
