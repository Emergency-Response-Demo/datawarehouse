package com.redhat.cajun.navy.datawarehouse;

import com.redhat.cajun.navy.datawarehouse.client.RespondersClient;
import com.redhat.cajun.navy.datawarehouse.dao.IReportingDAO;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.Responder;
import com.redhat.cajun.navy.datawarehouse.util.Constants;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.shareddata.LocalMap;

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DatawarehouseService {

    private static final Logger logger = LoggerFactory.getLogger(DatawarehouseService.class);

    @Inject
    io.vertx.mutiny.core.Vertx vertx;

    @Inject
    @RestClient
    RespondersClient respondersClient;

    @Inject
    IReportingDAO reportingDAO;

    void onStart(@Observes StartupEvent ev) {

        System.out.println("              _   _            _____    _____           ");
        System.out.println("             | \\ | |    /\\    |  __ \\  / ____|          ");
        System.out.println("             |  \\| |   /  \\   | |__) || (___            ");
        System.out.println("             | . ` |  / /\\ \\  |  ___/  \\___ \\           ");
        System.out.println("             | |\\  | / ____ \\ | |      ____) |          ");
        System.out.println("             |_| \\_|/_/    \\_\\|_|     |_____/           ");
        System.out.println("  ______  _____          _____                          ");
        System.out.println(" |  ____||  __ \\        |  __ \\                         ");
        System.out.println(" | |__   | |__) |______ | |  | |  ___  _ __ ___    ___  ");
        System.out.println(" |  __|  |  _  /|______|| |  | | / _ \\| '_ ` _ \\  / _ \\ ");
        System.out.println(" | |____ | | \\ \\        | |__| ||  __/| | | | | || (_) |");
        System.out.println(" |______||_|  \\_\\       |_____/  \\___||_| |_| |_| \\___/ ");
        System.out.println("                                                        ");
        System.out.println("                                    Powered by Red Hat  ");

        int numRespondersSeeded = seedResponderMap();
        logger.info("onStart() # of responders pulled from responder service = " + numRespondersSeeded);

    }

    /*
     * Datawarehouse should include Responder details At start-up populate a local
     * cache of Responders. Upon persistence of MissionReport, include details of
     * corresponding Responder
     */
    public int seedResponderMap() {
        Set<Responder> responders = respondersClient.available();
        LocalMap<Integer, Responder> responderMap = vertx.getDelegate().sharedData().getLocalMap(Constants.RESPONSE_MAP);
        logger.info("seedResponderMap() # of responders = " + responders.size() + "  : responderMap = " + responderMap);

        for (Responder rObj : responders) {
            Integer id = rObj.getId();
            responderMap.put(id, rObj);
        }
        int rCount = responders.size();
        return rCount;
    }

    public void processMissionStart(MissionReport mReport) {
        // Retrieve local cache of MissionReports
        LocalMap<String, MissionReport> mMap = vertx.getDelegate().sharedData().getLocalMap(Constants.MISSION_MAP);

        // Add this MissionReport to local cache
        mMap.put(mReport.getId(), mReport);

        // Update MissionReport with responder info details
        populateMissionReportWithResponderInfo(mReport);

        /*
         * Some ER-Demo events provide incidentId as the unique identifer and do not
         * contain missionId. Populate local INCIDENT_MISSION_MAP<incidentId,
         * missionId>. Later,will be able to retrieve missionId (using incidentId) and
         * process those incident events.
         */
        LocalMap<String, String> imMap = vertx.getDelegate().sharedData().getLocalMap(Constants.INCIDENT_MISSION_MAP);
        imMap.put(mReport.getIncidentId(), mReport.getId());
    }

    public void processMissionCompletion(MissionReport latestReport) {

        LocalMap<String, MissionReport> mMap = vertx.getDelegate().sharedData().getLocalMap(Constants.MISSION_MAP);
        MissionReport mReport = mMap.get(latestReport.getId());

        // Check if local cache contains corresponding MissionReport
        if (mReport == null) {
            logger.error(Constants.NO_REPORT_FOUND_EXCEPTION + " : " + latestReport.getId()
                    + " : No report found in local cache.  Will not persist because there would be fields missing in the report");
        } else {
            if(StringUtils.isEmpty(mReport.getProcessInstanceId())) {
                logger.error(Constants.NO_PROCESS_INSTANCE_ID_EXCEPTION
                        + "  :  Will not persist. No pInstanceId found for CreateMissionCommand with incidentId = "
                        + latestReport.getId());
            }else {

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
                        onResultCallback -> logger.info("processMissionCompletion: number of MissionReports persisted to database: "+ onResultCallback),
                        onFailureCallback -> onFailureCallback.printStackTrace()
                    );
            }

    
            // Delete temp report from MissionReportMap
            mMap.remove(mReport.getId());
        }

    }

    private void handleUni(Integer x) {

    }

    private void populateMissionReportWithResponderInfo(MissionReport mReport) {
        Integer id = Integer.parseInt(mReport.getResponderId());
        Responder rObj = (Responder) vertx.sharedData().getLocalMap(Constants.RESPONSE_MAP).get(id);
        if (rObj == null) {
            logger.warn("populateMissionReportWithResponderInfo() Did not originally find responder; Id = " + id);
            rObj = respondersClient.getById(id);
            vertx.sharedData().getLocalMap(Constants.RESPONSE_MAP).put(id, rObj);
        }
        mReport.setResponderFullName(rObj.getName());
        mReport.setResponderHasMedicalKit(rObj.getMedicalKit());
    }

    public void flushAllMissionReports() {
        LocalMap<String, MissionReport> mMap = vertx.getDelegate().sharedData().getLocalMap(Constants.MISSION_MAP);
        logger.info("flushAllMissionReports:  about to flush the following number of missionReports from cache: "+mMap.size());
        mMap.clear();

        reportingDAO.flushMissionReportTable()
            .subscribe().with(
                onResultCallback -> logger.info("flushAllMissionReports: number of MissionReports flushed from database: "+ onResultCallback),
                onFailureCallback -> onFailureCallback.printStackTrace()
            );
    }

    void onStop(@Observes ShutdownEvent ev) {
        logger.info("onStop() stopping...");
    }

}
