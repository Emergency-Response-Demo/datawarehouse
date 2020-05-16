package com.redhat.cajun.navy.datawarehouse.client;

import com.redhat.cajun.navy.datawarehouse.DatawarehouseService;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.cmd.mission.MissionCommand;
import io.vertx.core.json.Json;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 *  Consumes the following messages from "topic-mission-event" kafka topic:
 *    1)  MissionCompletedEvent   from MissionService
 *    2)  MissionStartedEvent     from MissionService
 *    3)  MissionPickedUpEvent    from MissionService
 */
@ApplicationScoped
public class TopicMissionEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicMissionEventConsumer");
    private static final String LOG_MISSION_EVENT_CONSUMER = "er.demo.LOG_MISSION_EVENT_COMSUMER";
    private boolean log = false;

    @Inject
    DatawarehouseService dwhService;

    @Inject
    @RestClient
    RespondersClient respondersClient;

    @Inject
    @ConfigProperty(name = LOG_MISSION_EVENT_CONSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-mission-event")
    public void process(String missionEventCommand) {
        if (StringUtils.isEmpty(missionEventCommand)) {
            logger.warn("process() empty message body");
            return;
        }
        if (this.log) {
            logger.info("process() topic-mission-event = " + missionEventCommand);
        }
        MissionCommand mcObj = Json.decodeValue(missionEventCommand, MissionCommand.class);
        String messageType = mcObj.getMessageType();


        if (messageType.equals(MissionCommand.MessageTypes.MissionStartedEvent.name())) {

            MissionReport mReport = mcObj.getMissionReport();
            dwhService.processMissionStart(mReport);
        
        } else if (messageType.equals(MissionCommand.MessageTypes.MissionCompletedEvent.name())) {
            try {
                MissionReport mReport = mcObj.getMissionReport();
                dwhService.processMissionCompletion(mReport);
            } catch (Throwable x) {
                // Don't throw any RuntimeExceptions; appears verticle goes stale thereafter
                x.printStackTrace();
            }
        } else {
            logger.info("process() messageType = " + messageType);
        }
    }

}
