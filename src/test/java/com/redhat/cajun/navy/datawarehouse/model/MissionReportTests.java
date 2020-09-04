package com.redhat.cajun.navy.datawarehouse.model;

import com.redhat.cajun.navy.datawarehouse.model.ResponderLocationUpdate;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import io.vertx.core.json.Json;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;
import com.redhat.cajun.navy.datawarehouse.model.cmd.mission.MissionCommand;
import com.redhat.cajun.navy.datawarehouse.util.CalculationException;

public class MissionReportTests {

    private static String missionStartedEventFilePath = "src/test/resources/MissionStartedEvent2.json";
    private static String missionCompletedEventFilePath = "src/test/resources/MissionCompletedEvent2.json";
    private static String topicResponderLocationUpdatePickupFilePath = "src/test/resources/topicResponderLocationUpdate_PICKEDUP2.json";

    //@Test
    public void decodeMissionCommandTest() throws IOException {
        int expectedNumSteps = 31;
        File testFile = new File(missionStartedEventFilePath);
        FileReader fStream = new FileReader(testFile);
        MissionCommand mcObj = Json.decodeValue(IOUtils.toString(fStream), MissionCommand.class);
        Assertions.assertEquals(expectedNumSteps, mcObj.getMissionReport().getSteps().size());
    }

    //@Test
    public void pickUpandDropOffDistancesTest() throws IOException, CalculationException {
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
