package com.rajanainart.common.integration.task;

import com.rajanainart.common.data.Database;
import com.rajanainart.common.integration.IntegrationConfig;
import com.rajanainart.common.integration.IntegrationContext;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Map;

public interface IntegrationTask {
    enum Status {
        PROCESSING(0),
        SUCCESS_COMPLETE(1),
        FAILURE_COMPLETE(2),
        WARNING_COMPLETE(3),
        NO_RUN(4);

        private final int value;
        private Status(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    void   setup  (IntegrationContext context );
    Status process(DelegateTransform transform);
    Status currentStatus();

    default void executeDependentTask(IntegrationContext context, IntegrationTask.DelegateTransform transform) {
        IntegrationConfig.TaskConfig t = context.getTaskConfig(context.getTaskConfig().getExecValue());
        if (t != null) {
            IntegrationContext newContext = new IntegrationContext(context.getConfig(), context.getLogger(), t.getId());
            newContext.setRestQueryRequest(context.getRestQueryRequest());
            String          key  = String.format("integration-task-%s", newContext.getTaskConfig().getType().toString().toLowerCase());
            IntegrationTask task = newContext.getIntegrationTasks().getOrDefault(key, null);
            if (task != null) {
                try {
                    task.setup  (newContext);
                    task.process(transform );
                }
                catch (Exception ex) {
                    context.getLogger().log("Exception while executing dependent task of integration config %s", context.getConfig().getId());
                    ex.printStackTrace();
                }
                finally {
                    newContext.close();
                }
            }
        }
    }

    default void initRuntimeHook(IntegrationContext context, String msg) {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.out.println(msg);

                try {
                    Connection connection = Database.getAdhocJdbcConnection();
                    try (Statement statement = connection.createStatement()) {
                        String     query     = String.format("INSERT INTO CMN_INTEGRATION_PROCESS_LOG (process_id, log, as_on) VALUES(%s, '%s', CURRENT_TIMESTAMP)", context.getLogger().getProcessId(), msg);
                        statement.execute(query);
                        query = String.format("UPDATE CMN_INTEGRATION_PROCESS SET status = 1 WHERE process_id = %s", context.getLogger().getProcessId());
                        statement.execute(query);
                        connection.commit();
                    }
                    Database.closeAdhocConnection();
                }
                catch(Exception ex) {
                    System.out.println("Exception while executing runtime hook");
                    ex.printStackTrace();
                }
            }
        });
    }

    @FunctionalInterface
    interface DelegateTransform {
        void process(IntegrationContext context, Map<String, Object> result);
    }
}
