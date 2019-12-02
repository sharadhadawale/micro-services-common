package com.rajanainart.common.integration;

import com.rajanainart.common.data.Database;
import com.rajanainart.common.integration.task.IntegrationTask;

import java.io.Closeable;

public class IntegrationLog implements Closeable {
    private Database          db    ;
    private IntegrationConfig config;
    private long              id    ;

    public long getProcessId() { return id; }

    public IntegrationLog(IntegrationConfig config) {
        this.db     = new Database();
        this.config = config;
        createProcess();
    }

    public Database getLoggerDb() { return db; }

    public void createProcess() {
        if (id != 0) return;

        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO CMN_INTEGRATION_PROCESS (CONFIG_NAME, STATUS)\r\n")
                .append("VALUES (?p_name, ?p_status)");

        db.executeQueryWithJdbc(builder.toString(),
                db.new Parameter("p_name"  , config.getId()),
                db.new Parameter("p_status", IntegrationTask.Status.PROCESSING.getValue()));
        id = db.selectCurrentSequenceValue("CMN_INTEGRATION_PROCESS");
        db.commit();
    }

    public void completeProcess(IntegrationTask.Status status) {
        StringBuilder builder = new StringBuilder();
        builder.append("UPDATE CMN_INTEGRATION_PROCESS SET status = ?p_status WHERE process_id = ?p_id");

        db.executeQueryWithJdbc(builder.toString(),
                db.new Parameter("p_id" , id),
                db.new Parameter("p_status", status.getValue()));
        db.commit();
    }

    public void log(String message) {
        System.out.println(message); //due to fortify "log forging" issue, keeping system.out
        StringBuilder builder = new StringBuilder();
        builder.append("INSERT INTO CMN_INTEGRATION_PROCESS_LOG (process_id, log, as_on)\r\n")
                .append("VALUES (?p_id, ?p_msg, current_timestamp)");

        try {
            db.executeQueryWithJdbc(builder.toString(),
                    db.new Parameter("p_id", id),
                    db.new Parameter("p_msg", message));
        }
        catch(Exception ex) {
            ex.printStackTrace();
        }
        db.commit();
    }

    @Override
    public void close() {
        if (db != null) db.close();
    }
}
