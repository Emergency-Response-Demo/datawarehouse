package com.redhat.cajun.navy.datawarehouse.client;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import com.redhat.cajun.navy.datawarehouse.model.Incident;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.cmd.incident.IncidentCommand;
import com.redhat.cajun.navy.datawarehouse.util.Constants;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.shareddata.LocalMap;
import io.vertx.core.json.Json;

/*
 Consumes the following events "topic-incident-event" Kafka topic:
   1) IncidentReportedEvent
   2) IncidentUpdatedEvent
*/
@ApplicationScoped
public class TopicIncidentEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger("TopicIncidentEventConsumer");
    private static final String LOG_INCIDENT_EVENT_CONSUMER = "er.dmo.LOG_INCIDENT_EVENT_COMSUMER";
    private boolean log = true;

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    @Inject
    @ConfigProperty(name = LOG_INCIDENT_EVENT_CONSUMER, defaultValue = "False")
    String logRawEvents;

    @PostConstruct
    public void start() {
        log = Boolean.parseBoolean(logRawEvents);
        logger.info("start() will log raw messaging events = " + log);
    }

    @Incoming("topic-incident-event")
    public void process(String topicIncidentEvent) {
        if (StringUtils.isEmpty(topicIncidentEvent)) {
            logger.warn("process() empty message body");
            return;
        }
        if (this.log) {
            logger.info("process() topicIncidentEvent = " + topicIncidentEvent);
        }

        if (StringUtils.contains(topicIncidentEvent, IncidentCommand.MessageTypes.IncidentUpdatedEvent.name())) {
            IncidentCommand icObj = Json.decodeValue(topicIncidentEvent, IncidentCommand.class);
            Incident iObj = icObj.getBody();
            if (Incident.Statuses.PICKEDUP.name().equals(iObj.getStatus())) {

                // If event = PICKEDUP, then update MissionReport with numberRescued

                // Retrieve local cache of incidentId -> missionId mapper
                LocalMap<String, String> imMap = vertx.getDelegate().sharedData().getLocalMap(Constants.INCIDENT_MISSION_MAP);
                String missionId = imMap.get(iObj.getId());
                if(StringUtils.isEmpty(missionId))
                    throw new RuntimeException(Constants.NO_REPORT_FOUND_EXCEPTION+" : No MissionReport found for reportId = "+missionId);

                // Retrive local cache of MissionReports
                LocalMap<String, MissionReport> mMap = vertx.getDelegate().sharedData().getLocalMap(Constants.MISSION_MAP);
                MissionReport mReport = mMap.get(missionId);

                mReport.setNumberRescued(iObj.getNumberOfPeople());

                // In addition, set MissionReport with previously determined processInstanceId
                LocalMap<String, String> ipMap = vertx.getDelegate().sharedData()
                        .getLocalMap(Constants.INCIDENT_PROCESS_INSTANCE_MAP);
                String pInstanceId = ipMap.get(iObj.getId());
                if (StringUtils.isNotEmpty(pInstanceId)) {
                    mReport.setProcessInstanceId(pInstanceId);
                } else {
                    throw new RuntimeException(Constants.NO_PROCESS_INSTANCE_ID_EXCEPTION
                            + "  :  No pInstanceId found for incidentId = " + iObj.getId());
                }
            }

        }

    }

}
