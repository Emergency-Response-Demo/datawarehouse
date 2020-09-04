package com.redhat.cajun.navy.datawarehouse.model;

import org.infinispan.protostream.SerializationContextInitializer;
import org.infinispan.protostream.annotations.AutoProtoSchemaBuilder;

@AutoProtoSchemaBuilder(
    includeClasses = {
        com.redhat.cajun.navy.datawarehouse.model.cmd.mission.Body.class,
        com.redhat.cajun.navy.datawarehouse.model.Responder.class,
        com.redhat.cajun.navy.datawarehouse.model.MissionReport.class,
        com.redhat.cajun.navy.datawarehouse.model.ResponderLocationHistory.class,
        com.redhat.cajun.navy.datawarehouse.model.MissionStep.class
    }
)
interface DatawarehouseContextInitializer extends SerializationContextInitializer {
}