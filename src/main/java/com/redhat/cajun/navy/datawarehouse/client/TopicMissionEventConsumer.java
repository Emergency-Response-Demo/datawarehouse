package com.redhat.cajun.navy.datawarehouse.client;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.MessageProcessingService;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.cmd.mission.MissionCommand;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.smallrye.reactive.messaging.ce.IncomingCloudEventMetadata;
import io.smallrye.reactive.messaging.kafka.IncomingKafkaRecord;
import io.vertx.core.json.Json;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Acknowledgment;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*  Purpose:
 *     Consume MissionCompletedEvent which contains list of responderLocationHistory (each with timestamps)
 *     This message is produced by mission service
 * 
 */
@ApplicationScoped
public class TopicMissionEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicMissionEventConsumer");
    private static final String LOG_MISSION_EVENT_CONSUMER = "er.demo.LOG_MISSION_EVENT_COMSUMER";
    private boolean log = false;

    @Inject
    MessageProcessingService mService;

    @Inject
    @ConfigProperty(name = LOG_MISSION_EVENT_CONSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-mission-event")
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
        if (this.log) {
            logger.info("process() topic-mission-event = " + message.getPayload());
        }
        MissionReport mReport = Json.decodeValue(message.getPayload(), MissionReport.class);
        String messageType = cloudEventMetadata.getType();

        if (messageType.equals(MissionCommand.MessageTypes.MissionCompletedEvent.name())) {
            try {
                mService.processMissionCompletion(mReport);
            } catch (Throwable x) {
                logger.error("Error processing MissionCompletedEvent() incidentId = "+ mReport.incidentId);
                // Don't throw any RuntimeException
                //  It is important to actually handle the exception
                // If the exception bubbles up to the Reactive Messaging Kafka connector it would cause the connector to shut down and stop consuming messages.
                x.printStackTrace();
            }
        } else {
            if (this.log) {
                logger.info("process() messageType = " + messageType);
            }
        }
        return CompletableFuture.completedFuture(null);
    }

}
