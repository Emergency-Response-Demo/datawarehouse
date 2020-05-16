package com.redhat.cajun.navy.datawarehouse.dao;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.ResponderLocationUpdate;
import com.redhat.cajun.navy.datawarehouse.model.cmd.mission.MissionCommand;
import io.quarkus.test.junit.QuarkusTest;
import io.vertx.core.json.Json;
import java.io.File;
import java.io.FileReader;
import java.util.concurrent.CompletionStage;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@QuarkusTest
public class MissionReportDAOTests {

    private static String missionCompletedEventFilePath = "src/test/resources/MissionCompletedEvent2.json";
    private static String topicResponderLocationUpdatePickupFilePath = "src/test/resources/topicResponderLocationUpdate_PICKEDUP2.json";
    private static final Logger logger = LoggerFactory.getLogger(MissionReportDAOTests.class);

    //@Inject
    IReportingDAO reportingDAO;

    /*  Currently assumes existing of a postgresql rdbms at: 127.0.0.1:5432/datawarehouse
     */
    //@Test
    public void persistMissionReportTest() throws Exception {

        Assertions.assertNotNull(reportingDAO);

        double expectedPickupDistance = 11925.486164264421;
        double expectedDropoffDistance = 14117.114430289823;

        File testFile = new File(missionCompletedEventFilePath);
        FileReader fStream = new FileReader(testFile);
        MissionCommand mcObj = Json.decodeValue(IOUtils.toString(fStream), MissionCommand.class);
        fStream.close();

        testFile = new File(topicResponderLocationUpdatePickupFilePath);
        fStream = new FileReader(testFile);
        ResponderLocationUpdate rlObj = Json.decodeValue(IOUtils.toString(fStream), ResponderLocationUpdate.class);
        fStream.close();

        MissionReport mReport = mcObj.getMissionReport();
        mReport.setPickupLat(rlObj.getLat());
        mReport.setPickupLong(rlObj.getLon());

        mReport.calculateDistancesAndTimes();
        Assertions.assertEquals(expectedPickupDistance, mReport.getResponderDistancePickup(), 0.0);
        Assertions.assertEquals(expectedDropoffDistance, mReport.getResponderDistanceDropoff(), 0.0);
    }

}
