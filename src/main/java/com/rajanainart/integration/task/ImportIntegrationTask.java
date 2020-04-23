package com.rajanainart.integration.task;

import com.rajanainart.concurrency.ConcurrencyManager;
import com.rajanainart.config.AppContext;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.integration.IntegrationConfig;
import com.rajanainart.integration.IntegrationContext;
import com.rajanainart.integration.querybuilder.DbSpecficQueryBuilder;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;
import com.rajanainart.rest.RestQueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.sql.ResultSet;
import java.util.*;

@Component("integration-task-import")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ImportIntegrationTask implements IntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(ImportIntegrationTask.class);

    public  final static Map<String, DbSpecficQueryBuilder> DB_SPECIFIC_QUERY_BUILDERS = AppContext.getBeansOfType(DbSpecficQueryBuilder.class);

    protected IntegrationContext context  = null;
    protected Status             current  = Status.PROCESSING;

    @Override
    public void setup(IntegrationContext context) {
        this.context = context;
    }

    @Override
    public Status currentStatus() {
        return current;
    }

    @Override
    public Status process(IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String msg = String.format("IMPORT: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(msg);
        processRestQuery(context.getTaskConfig().getExecValue(), transform);
        return current;
    }

    public void processRestQuery(String query, IntegrationTask.DelegateTransform transform) {
        RestQueryConfig config   = BaseRestController.REST_QUERY_CONFIGS.get(query);
        String          valid    = validateRestConfig(config);
        if (!valid.equalsIgnoreCase(BaseRestController.SUCCESS)) {
            context.getLogger().log(valid);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        String                key      = String.format("query-builder-%s", context.getTargetDb().getJdbcDriverPropertyValue());
        DbSpecficQueryBuilder qBuilder = DB_SPECIFIC_QUERY_BUILDERS.getOrDefault(key, null);
        if (qBuilder == null) {
            String msg = String.format("No DbSpecificQueryBuilder found for the target %s", context.getTargetDb().getDbKey());
            context.getLogger().log(msg);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        processTask(config, qBuilder, transform);

        context.getTargetDb().commit();
        if (current == Status.PROCESSING)
            current = Status.SUCCESS_COMPLETE;
    }

    private void processTask(RestQueryConfig config, DbSpecficQueryBuilder qBuilder, IntegrationTask.DelegateTransform transform) {
        context.getLogger().log("Executing pre-tasks, if any");
        executePrePostTasks(context.getTaskConfig().getPreExecValue(), transform);

        context.getLogger().log("Fetching records from source");
        QueryExecutor  executor  = new QueryExecutor(config, context.getRestQueryRequest(), context.getSourceDb());
        long           startTime = System.currentTimeMillis();
        List<Long> totalRecords  = new ArrayList<>();
        List<Map<String, Object>> records   = new ArrayList<>();
        try (ConcurrencyManager concurrency = new ConcurrencyManager("integration-import-task")) {
            processDelete(qBuilder, config);
            if (current == Status.FAILURE_COMPLETE) return;

            totalRecords.add(-1L);
            executor.selectWithCallback((record, index) -> {
                buildRecords(config, records, record);

                if ((index+1) % 1000 == 0)
                    context.getLogger().log(String.format("Processing records %s, time elapsed in seconds %s", index+1,
                            (System.currentTimeMillis() - startTime)/1000));
                if ((index+1) % context.getTaskConfig().getBulkCount() == 0) {
                    List<Map<String, Object>> processRecords = new ArrayList<>();
                    processRecords.addAll(records);
                    records.clear();
                    ImportThread imp = new ImportThread(processRecords, transform, qBuilder, config);
                    concurrency.submit(imp);
                }
                totalRecords.set(0, index);
            });
            if (records.size() > 0) {
                ImportThread imp = new ImportThread(records, transform, qBuilder, config);
                concurrency.submit(imp);
            }
            await(concurrency, totalRecords, transform);
        }
    }

    protected void await(ConcurrencyManager concurrency, List<Long> totalRecords,
                         IntegrationTask.DelegateTransform transform) {
        context.getLogger().log("Waiting for the threads to complete");
        concurrency.awaitTermination((message -> context.getLogger().log(message)), 100);

        context.getLogger().log(String.format("Total processed records %s", totalRecords.get(0)+1));
        context.getTargetDb().commit();

        context.getLogger().log("Executing post-tasks, if any");
        executePrePostTasks(context.getTaskConfig().getPostExecValue(), transform);
    }

    protected void buildRecords(RestQueryConfig config, List<Map<String, Object>> records, ResultSet record) {
        Map<String, Object> r = new HashMap<>();
        try {
            for (RestQueryConfig.FieldConfig f : config.getFields()) {
                if (f.getAutoIncr()) continue;
                r.put(f.getId(), record.getObject(f.getId()));
            }
            records.add(r);
        }
        catch (Exception ex) {
            current = Status.WARNING_COMPLETE;
            context.getLogger().log("Error while reading record from source");
            ex.printStackTrace();
        }
    }

    protected void executePrePostTasks(String[] tasks, IntegrationTask.DelegateTransform transform) {
        String actualTask = context.getTask();
        for (String t : tasks) {
            RestQueryConfig c = BaseRestController.REST_QUERY_CONFIGS.get(t);
            if (c != null) {
                QueryExecutor e = new QueryExecutor(c, context.getRestQueryRequest(), context.getTargetDb());
                e.executeQuery();
            }
            else {
                try {
                    IntegrationConfig.TaskConfig tc = context.getTaskConfig(t);
                    String          key  = String.format("integration-task-%s", tc.getType().name().toLowerCase());
                    IntegrationTask task = context.getIntegrationTasks().getOrDefault(key, null);
                    if (task != null) {
                        context.setTask(t);
                        task.setup  (context);
                        task.process(transform);
                    }
                }
                catch (Exception ex) {
                    context.getLogger().log("Exception occurred while executing pre/post tasks");
                    ex.printStackTrace();
                }
            }
        }
        context.setTask(actualTask);
    }

    private void processDelete(DbSpecficQueryBuilder qBuilder, RestQueryConfig config) {
        try {
            String deleteQuery = qBuilder.delete(config, context.getRestQueryRequest().getParams());
            if (!deleteQuery.isEmpty()) {
                context.getLogger().log("Deleting existing rows");
                //log.info(deleteQuery);
                context.getTargetDb().executeQueryWithJdbc(deleteQuery);
            }
        }
        catch (IllegalArgumentException ex) {
            current = Status.FAILURE_COMPLETE;
            context.getLogger().log(ex.getMessage());
            ex.printStackTrace();
            return;
        }
    }

    public String validateRestConfig(RestQueryConfig config) {
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML)
            return String.format("Import REST config can't be DML %s", config.getId());
        if (config.getTarget().isEmpty())
            return String.format("Target is mandatory in the REST config %s", config.getId());
        if (config.getFields().size() == 0)
            return String.format("No fields defined in the REST config %s", config.getId());
        DefaultEntityHandler handler = new DefaultEntityHandler();
        return handler.preValidateRestEntity(config, context.getRestQueryRequest());
    }

    public class ImportThread implements ConcurrencyManager.BaseConcurrencyThread {
        private List<Map<String, Object>>         records  ;
        private IntegrationTask.DelegateTransform transform;
        private DbSpecficQueryBuilder             qBuilder ;
        private RestQueryConfig                   config   ;
        private boolean  complete = false;
        private String   name     = "";

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }

        public ImportThread(List<Map<String, Object>> records, IntegrationTask.DelegateTransform transform,
                            DbSpecficQueryBuilder qBuilder, RestQueryConfig config) {
            this.records   = records  ;
            this.transform = transform;
            this.qBuilder  = qBuilder ;
            this.config    = config   ;
        }

        @Override
        public void run() {
            name = String.format("%s-%s", Thread.currentThread().getName(), new SecureRandom().nextLong());
            String finalQuery = "";
            try {
                for (Map<String, Object> record : records)
                    transform.process(context, record);

                finalQuery = qBuilder.bulkUpdate(config, records);
                context.getTargetDb().executeQueryWithJdbc(finalQuery);
            }
            catch (Exception ex) {
                current = Status.FAILURE_COMPLETE;
                String msg = String.format("Error while updating the data,thread:%s,records:%s,%s", name, records.size(), ex.getMessage());
                log.error(finalQuery);
                ex.printStackTrace();
                context.getLogger().log(msg);
            }
            complete = true;
        }
    }
}
