package com.redhat.cajun.navy.datawarehouse.dao;

import com.redhat.cajun.navy.datawarehouse.model.MissionReport;

import io.quarkus.runtime.ShutdownEvent;
import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.sqlclient.SqlConnection;

import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/* 
 * Datawarehouse reporting data access object that uses Vertx based reactive postgresql client.
 * 
 * Reference:
 *   https://vertx.io/docs/vertx-pg-client/java/
 *   https://quarkus.io/guides/reactive-sql-clients
 */
@ApplicationScoped
public class ReportingReactiveDAO implements IReportingDAO {

    private static final Logger logger = LoggerFactory.getLogger("ReportingReactiveDAO");
    private static final String QUARKUS_DATASOURCE_URL = "quarkus.datasource.url";

    private static final String SINGLE_QUOTE = "'";
    private static final String COMMA = ",";

    @Inject
    io.vertx.mutiny.pgclient.PgPool pgClient;

    @Inject
    @ConfigProperty(name = QUARKUS_DATASOURCE_URL)
    String datasourceURL;

    
    public void onStart(@Observes @Priority(value = 1) StartupEvent ev) {
        logger.info("start() reactive pg client = " + pgClient + " : datasourceUrl = " + datasourceURL);
        pgClient.query("select 1").execute().onItem().transform(rSet -> rSet.rowCount());
    }

    @Override
    public Uni<Integer> persistMissionReport(MissionReport missionReport) {

        String insertString = createInsertMissionReportSQL(missionReport);
        return pgClient.query(insertString)
                        .execute()
                        .onItem()
                        .transform((pgRowSet) -> pgRowSet.rowCount());
    }

    public Uni<Integer> flushMissionReportTable() {
        return pgClient.query("delete from MissionReport")
                        .execute()
                        .onItem()
                        .transform((pgRowSet) -> pgRowSet.rowCount());
    }

    private String createInsertMissionReportSQL(MissionReport mReport) {
        StringBuilder sBuilder = new StringBuilder("insert into MissionReport values(");
        sBuilder.append(SINGLE_QUOTE + mReport.getId() + SINGLE_QUOTE + COMMA);
        sBuilder.append(SINGLE_QUOTE + mReport.getStatus() + SINGLE_QUOTE + COMMA);
        sBuilder.append(SINGLE_QUOTE + mReport.getIncidentId() + SINGLE_QUOTE + COMMA);
        sBuilder.append(SINGLE_QUOTE + mReport.getProcessInstanceId() + SINGLE_QUOTE + COMMA);
        sBuilder.append(mReport.getResponderId() + COMMA);
        sBuilder.append(SINGLE_QUOTE + mReport.getResponderFullName() + SINGLE_QUOTE + COMMA);
        sBuilder.append(mReport.isResponderHasMedicalKit() + COMMA);
        sBuilder.append(mReport.getNumberRescued() + COMMA);
        sBuilder.append(mReport.getResponderDistancePickup() + COMMA);
        sBuilder.append(mReport.getResponderDistanceDropoff() + COMMA);
        sBuilder.append(mReport.getResponderDistanceTotal() + COMMA);
        sBuilder.append(mReport.getResponseTimeSecondsPickup() + COMMA);
        sBuilder.append(mReport.getResponseTimeSecondsDropoff() + COMMA);
        sBuilder.append(mReport.getResponseTimeSecondsTotal());
        sBuilder.append(")");
        return sBuilder.toString();
    }


    void onStop(@Observes ShutdownEvent ev) {
        logger.info("end()");
        pgClient.close();
    }

}