package com.redhat.cajun.navy.datawarehouse.client;

import com.redhat.cajun.navy.datawarehouse.util.Constants;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.model.cmd.createMissionCommand.Body;
import com.redhat.cajun.navy.datawarehouse.model.cmd.createMissionCommand.CreateMissionCommand;
import com.redhat.cajun.navy.datawarehouse.model.cmd.mission.MissionCommand;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.json.Json;

/*
 *   Consumes the following message type on the topic-mission-command kafka topic: 
 *
 *   {"id":"eece9ab4-9cb6-469b-b7f1-2f490dae599a","messageType":"CreateMissionCommand","invokingService":"IncidentProcessService","timestamp":1579820261957,"body":{"incidentId":"97b48ce8-2bf2-47c2-879c-b8ba8468c01c","responderId":"189","responderStartLat":"34.24630","responderStartLong":"-77.95140","incidentLat":"34.16423","incidentLong":"-77.85931","destinationLat":"34.17060","destinationLong":"-77.94900","processId":"220"}}
 * 
 */
@ApplicationScoped
public class TopicMissionCommandConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicMissionCommandConsumer");
    private static final String LOG_MISSION_COMMAND_COMSUMER = "er.demo.LOG_MISSION_COMMAND_COMSUMER";
    private boolean log = true;

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    @Inject
    @ConfigProperty(name = LOG_MISSION_COMMAND_COMSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-mission-command")
    public void process(String topicCommand) {
        if (StringUtils.isEmpty(topicCommand)) {
            logger.warn("process() empty message body");
            return;
        }
        if (this.log) {
            logger.info("process() topic-mission-command = " + topicCommand);
        }

        if (StringUtils.contains(topicCommand, MissionCommand.MessageTypes.CreateMissionCommand.name())) {
            CreateMissionCommand icObj = Json.decodeValue(topicCommand, CreateMissionCommand.class);
            Body iObj = icObj.getBody();
            String pInstanceId = iObj.getProcessId();
            if (StringUtils.isNotEmpty(pInstanceId)) {

                /*
                 * Populate local cache map<String incidentId, processInstanceId> Doing so will
                 * allow later correlation between incidentId, processInstanceId and missionId
                 */

                // Retrieve local cache of incidentId -> missionId mapper
                LocalMap<String, String> ipMap = vertx.getDelegate().sharedData()
                        .getLocalMap(Constants.INCIDENT_PROCESS_INSTANCE_MAP);
                ipMap.put(iObj.getIncidentId(), pInstanceId);
            } else {
                logger.error(Constants.NO_PROCESS_INSTANCE_ID_EXCEPTION
                        + "  :  No pInstanceId found for CreateMissionCommand with incidentId = "
                        + iObj.getIncidentId());
            }

        }
    }

}