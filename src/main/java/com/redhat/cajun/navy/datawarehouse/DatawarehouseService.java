package com.redhat.cajun.navy.datawarehouse;

import com.redhat.cajun.navy.datawarehouse.client.RespondersClient;
import com.redhat.cajun.navy.datawarehouse.dao.IReportingDAO;
import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.Responder;
import com.redhat.cajun.navy.datawarehouse.util.Constants;
import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;

import java.util.Set;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.annotation.Priority;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.configuration.XMLStringConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class DatawarehouseService {

    private static final Logger logger = LoggerFactory.getLogger(DatawarehouseService.class);
    private static final String DELETE_REMOTE_CACHE_ON_STARTUP =  "er.demo.DELETE_REMOTE_CACHE_ON_STARTUP";

    private static final String RESPONDER_CACHE_CONFIG =
    "<infinispan><cache-container>" +
            "<distributed-cache name=\"%s\"></distributed-cache>" +
            "</cache-container></infinispan>";

    /*  This infinispan cache is configured to prevent write-skews (as documented in here:  https://red.ht/2Zf5A7j)
     *  write-skews can occur during the pick-up event when consumption of the following two messages occurs:
     *    1) topic-incident-event              :   IncidentUpdatedEvent (produced by incident-service) with number of people actually picked up
     *    2) topic-responder-location-update   :   Consumes a location-update message (with status of PICKEDUP and lat/lon of pickup location)
     */
    /*
     * Optimistic locking only locks keys during the transaction commit
     * It throws a WriteSkewCheckException at commit time, if another transaction modified the same keys after the current transaction read them   
    */
    private static final String MISSION_CACHE_CONFIG_OPTIMISTIC =
            "<infinispan><cache-container><distributed-cache name=\"%s\">" +
                    "<locking isolation=\"READ_COMMITTED\"/>" +
                    "<transaction locking=\"OPTIMISTIC\" mode=\"NONE\" auto-commit=\"true\" " +
                    "transaction-manager-lookup=\"org.infinispan.transaction.lookup.GenericTransactionManagerLookup\" />"+
            "</distributed-cache></cache-container></infinispan>";

    /* Pessimistic locking locks keys on a write operation or when the user calls AdvancedCache.lock(keys) explicitly.  */
    private static final String MISSION_CACHE_CONFIG_PESSIMISTIC =
    "<infinispan><cache-container><distributed-cache name=\"%s\">" +
            "<locking isolation=\"REPEATABLE_READ\"/>" +
            "<transaction locking=\"PESSIMISTIC\" mode=\"NON_XA\" auto-commit=\"false\" " +
            "transaction-manager-lookup=\"org.infinispan.transaction.lookup.GenericTransactionManagerLookup\" />"+
    "</distributed-cache></cache-container></infinispan>";


    @Inject
    @RestClient
    RespondersClient respondersClient;

    @Inject
    IReportingDAO reportingDAO;

    @Inject
    RemoteCacheManager cacheManager;

    @Inject
    @ConfigProperty(name = DELETE_REMOTE_CACHE_ON_STARTUP, defaultValue = "False")
    boolean deleteRemoteCacheOnStartup = false;

    void onStart(@Observes @Priority(value = 1) StartupEvent ev) {

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

    
        /* ************         Infinispan Server Side Configurations          *************** */
        /*
        if(deleteRemoteCacheOnStartup) {
            if(cacheManager.getCache(Constants.RESPONDER_MAP) != null)
                cacheManager.administration().removeCache(Constants.RESPONDER_MAP);
            logger.info("Justed deleted the following cache: "+Constants.RESPONDER_MAP);

            if(cacheManager.getCache(Constants.INCIDENT_MAP) != null)
                cacheManager.administration().removeCache(Constants.INCIDENT_MAP);
            logger.info("Justed deleted the following cache: "+Constants.INCIDENT_MAP);
        }
        String missionMapXml = String.format(MISSION_CACHE_CONFIG_PESSIMISTIC, Constants.INCIDENT_MAP);
        RemoteCache<String, MissionReport> mMap = cacheManager.administration().getOrCreateCache(Constants.INCIDENT_MAP, new XMLStringConfiguration(missionMapXml));

        String xml = String.format(RESPONDER_CACHE_CONFIG, Constants.RESPONDER_MAP);
        RemoteCache<Integer, Responder> responderMap = cacheManager.administration().getOrCreateCache(Constants.RESPONDER_MAP, new XMLStringConfiguration(xml));
        */

        /* ***************************************************************** */


        // Seed responder cache
        RemoteCache<Integer, Responder> responderMap = cacheManager.getCache(Constants.RESPONDER_MAP);
        int numRespondersSeeded = seedResponderMap(responderMap);

        StringBuilder sBuilder = new StringBuilder("onStart() cache names = ");
        Set<String> cacheNames = cacheManager.getCacheNames();
        for(String cacheName : cacheNames) {
            sBuilder.append("\n\t"+cacheName);
        }

        sBuilder.append("\n\nonStart() # of responders pulled from responder service = " + numRespondersSeeded +"\n\tresponderMap = "+responderMap);
        logger.info(sBuilder.toString());
    }

    /*
     * Datawarehouse should include Responder details at start-up 
     * Therefore, populate a local cache of Responders
     * Upon persistence of MissionReport, include details of corresponding Responder
     */
    public int seedResponderMap(RemoteCache<Integer, Responder> responderMap) {
        Set<Responder> responders = null;
        int rCount = 0;
        responders = respondersClient.available();
        for (Responder rObj : responders) {
            Integer id = rObj.getId();
            responderMap.put(id, rObj);
        }
        rCount = responders.size();
        return rCount;
    }

    public void flushAllMissionReportsFromDB() {

        reportingDAO.flushMissionReportTable()
            .subscribe().with(
                onResultCallback -> logger.info("flushAllMissionReports: number of MissionReports flushed from database: "+ onResultCallback),
                onFailureCallback -> onFailureCallback.printStackTrace()
            );
    }

    void onStop(@Observes ShutdownEvent ev) {
        logger.info("onStop() stopping...");
        cacheManager.close();
    }

}
