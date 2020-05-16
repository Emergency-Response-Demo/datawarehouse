package com.redhat.cajun.navy.datawarehouse.dao;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;

import io.smallrye.mutiny.Uni;

public interface IReportingDAO {

    // return 1 if persisted, 0 if not persisted
    public Uni<Integer> persistMissionReport(MissionReport missionReport);

    // return number of records deleted
    public Uni<Integer> flushMissionReportTable();

}
