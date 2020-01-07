package com.rajanainart.common.integration.task;

import com.rajanainart.common.concurrency.ConcurrencyManager;
import com.rajanainart.common.data.QueryExecutor;
import com.rajanainart.common.data.nosql.BaseNoSqlDataProvider;
import com.rajanainart.common.data.nosql.NoSqlConfig;
import com.rajanainart.common.integration.IntegrationManager;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("integration-task-nosql_import")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NoSqlDbImportIntegrationTask extends ImportIntegrationTask {

    @Override
    public void processRestQuery(String query, IntegrationTask.DelegateTransform transform) {
        RestQueryConfig config   = BaseRestController.REST_QUERY_CONFIGS.get(query);
        String          valid    = validateRestConfig(config);
        if (!valid.equalsIgnoreCase(BaseRestController.SUCCESS)) {
            context.getLogger().log(valid);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        NoSqlConfig noSqlConfig = IntegrationManager.NOSQL_CONFIGS.getOrDefault(context.getTaskConfig().getTarget(), null);
        BaseNoSqlDataProvider nosql       = null;
        if (noSqlConfig == null) {
            String msg = String.format("NoSql Db configuration is not found %s", context.getTaskConfig().getTarget());
            context.getLogger().log(msg);
            current = Status.FAILURE_COMPLETE;
            return;
        }
        else {
            String key = noSqlConfig.getType().toString().toLowerCase(Locale.ENGLISH);
            nosql      = context.getNoSqlDbProviders().getOrDefault(key, null);
        }
        if (nosql == null) {
            String msg = String.format("NoSql Data provider is not found %s", noSqlConfig.getType().toString());
            context.getLogger().log(msg);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        processTask(config, noSqlConfig, nosql, transform);
        if (current == Status.PROCESSING)
            current = Status.SUCCESS_COMPLETE;
    }

    private void processTask(RestQueryConfig config, NoSqlConfig noSqlConfig,
                             BaseNoSqlDataProvider nosql, IntegrationTask.DelegateTransform transform) {
        context.getLogger().log("Fetching records from source");
        QueryExecutor executor  = new QueryExecutor(config, context.getRestQueryRequest(), context.getSourceDb());
        long          startTime = System.currentTimeMillis();
        List<Long> totalRecords = new ArrayList<>();
        List<Map<String, Object>> records = new ArrayList<>();

        try (ConcurrencyManager concurrency = new ConcurrencyManager("integration-nosql-import-task")) {
            totalRecords.add(-1L);
            nosql.open(noSqlConfig, config);
            executor.selectWithCallback((record, index) -> {
                buildRecords(config, records, record);

                if ((index+1) % 1000 == 0)
                    context.getLogger().log(String.format("Processing records %s, time elapsed in seconds %s", index+1,
                            (System.currentTimeMillis() - startTime)/1000));
                if ((index+1) % context.getTaskConfig().getBulkCount() == 0) {
                    List<Map<String, Object>> processRecords = new ArrayList<>();
                    processRecords.addAll(records);
                    records.clear();
                    ImportThread imp = new ImportThread(processRecords, nosql, transform);
                    concurrency.submit(imp);
                }
                totalRecords.set(0, index);
            });
            if (records.size() > 0) {
                ImportThread imp = new ImportThread(records, nosql, transform);
                concurrency.submit(imp);
            }
            context.getLogger().log("Waiting for the threads to complete");
            concurrency.awaitTermination((message -> context.getLogger().log(message)), 100);
        }

        context.getLogger().log(String.format("Total processed records %s", totalRecords.get(0)+1));
    }

    public class ImportThread implements ConcurrencyManager.BaseConcurrencyThread {
        private List<Map<String, Object>>         records  ;
        private IntegrationTask.DelegateTransform transform;
        private BaseNoSqlDataProvider             nosql    ;

        private boolean  complete = false;
        private String   name     = "";

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }

        public ImportThread(List<Map<String, Object>> records, BaseNoSqlDataProvider nosql,
                            IntegrationTask.DelegateTransform transform) {
            this.records     = records  ;
            this.transform   = transform;
            this.nosql       = nosql    ;
        }

        @Override
        public void run() {
            name = String.format("%s-%s", Thread.currentThread().getName(), new SecureRandom().nextLong());

            try {
                for (Map<String, Object> record : records)
                    transform.process(context, record);

                nosql.bulkUpdate(records);
            }
            catch (Exception ex) {
                current = Status.FAILURE_COMPLETE;
                String msg = String.format("Error while updating the data,thread:%s,records:%s,%s", name, records.size(), ex.getMessage());
                ex.printStackTrace();
                context.getLogger().log(msg);
            }
            complete = true;
        }
    }
}
