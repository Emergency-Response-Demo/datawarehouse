package com.redhat.cajun.navy.datawarehouse;

import com.redhat.cajun.navy.datawarehouse.client.RespondersClient;
import com.redhat.cajun.navy.datawarehouse.dao.IReportingDAO;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.Responder;
import com.redhat.cajun.navy.datawarehouse.util.Constants;

import io.quarkus.infinispan.client.Remote;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.annotation.Priority;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class MessageProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(MessageProcessingService.class);

    @Inject
    @RestClient
    RespondersClient respondersClient;

    @Inject
    IReportingDAO reportingDAO;

    @Inject
    @Remote(Constants.INCIDENT_MAP)
    RemoteCache<String, MissionReport> mMap;

    @Inject
    @Remote(Constants.RESPONDER_MAP)
    RemoteCache<Integer, Responder> responderMap;

    void onStart(@Observes @Priority(value = 2) StartupEvent ev) {
        logger.info("onStart() starting...");
    }

    public void processMissionStart(MissionReport mReport) {

        String incidentId = null;
        try {
            incidentId = mReport.getIncidentId();
    
            // Update MissionReport with responder info details
            populateMissionReportWithResponderInfo(mReport);
    
            logger.info(incidentId + " : MissionReport added. pInstanceId = "+mReport.getProcessInstanceId());
    
            // Add this MissionReport to local cache
            mMap.put(mReport.getIncidentId(), mReport);

        } catch(Throwable x) {
            logger.error(incidentId +" : processMissionStart() following exception thrown :");
            x.printStackTrace();
        }
    }

    public void processMissionCompletion(MissionReport latestReport) {

        MetadataValue<MissionReport> mValue = mMap.getWithMetadata(latestReport.getIncidentId());
        
        // Check if local cache contains corresponding MissionReport
        if (mValue == null) {
            logger.error(Constants.NO_REPORT_FOUND_EXCEPTION + " : " + latestReport.getIncidentId()
            + " : No report found cache.  Will not persist because there would be fields missing.");
        } else {
            MissionReport mReport = mValue.getValue();
            if(StringUtils.isEmpty(mReport.getProcessInstanceId())) {
                logger.error(Constants.NO_PROCESS_INSTANCE_ID_EXCEPTION
                        + "  :  Will not persist. No pInstanceId found for CreateMissionCommand with incidentId = "
                        + latestReport.getIncidentId());
            }else {

                try { 

                    /*
                     * Original MissionReport from MissionStartedEvent has empty
                     * ResponderLocationHistory. Add ResponderLocationHistory from this
                     * MissionCompletedEvent to original MissionReport.
                     */
                    mReport.setResponderLocationHistory(latestReport.getResponderLocationHistory());
        
                    /*
                     * Based on ResponderLocationHistory, calculate distance travelled and
                     * durations.
                     */
                    mReport.calculateDistancesAndTimes();
        
                    /*
                     * Change status of original report
                     */
                    mReport.setStatus(latestReport.getStatus());
        
                    /*
                     * Persist original MissionReport (updated with latest data and calculations) to
                     * the MissionReport datawarehouse.
                     */
                    reportingDAO.persistMissionReport(mReport)
                        .subscribe().with(
                            onResultCallback -> logger.info(mReport.getIncidentId()+ " : MissionCompleted : number of MissionReports persisted to database: "+ onResultCallback),
                            onFailureCallback -> onFailureCallback.printStackTrace()
                        );

                }catch(Exception x) {
                    x.printStackTrace();
                }
            }

            // Delete temp report from MissionReportMap
            mMap.removeWithVersion(latestReport.getIncidentId(), mValue.getVersion());
        }
    }

    public void flushAllMissionReportsFromCache() {
        logger.info("flushAllMissionReportsFromCache:  about to flush the following number of missionReports from cache: "+mMap.size());
        mMap.clear();
    }

    private void populateMissionReportWithResponderInfo(MissionReport mReport) {
        Integer id = Integer.parseInt(mReport.getResponderId());
        Responder rObj = responderMap.get(id);
        if (rObj == null) {
            logger.warn("populateMissionReportWithResponderInfo() Did not originally find responder; Id = " + id);
            rObj = respondersClient.getById(id);
            responderMap.put(id, rObj);
        }
        mReport.setResponderFullName(rObj.getName());
        mReport.setResponderHasMedicalKit(rObj.getMedicalKit());
    }

    void onStop(@Observes ShutdownEvent ev) {
        logger.info("onStop() stopping...");
    }

}
