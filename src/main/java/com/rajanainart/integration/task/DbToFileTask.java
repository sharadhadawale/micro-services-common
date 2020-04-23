package com.rajanainart.integration.task;

import com.rajanainart.data.QueryExecutor;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.resource.FileConfig;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;
import com.rajanainart.rest.RestQueryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("integration-task-file")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DbToFileTask extends ImportIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DbToFileTask.class);

    @Override
    public IntegrationTask.Status process (IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String m = String.format("FILE: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(m);
        processRestQuery(context.getTaskConfig().getExecValue(), transform);
        return current;
    }

    @Override
    public void processRestQuery(String query, IntegrationTask.DelegateTransform transform) {
        RestQueryConfig config   = BaseRestController.REST_QUERY_CONFIGS.get(query);
        String          valid    = validateRestConfig(config);
        if (!valid.equalsIgnoreCase(BaseRestController.SUCCESS)) {
            context.getLogger().log(valid);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        FileConfig fileConfig = IntegrationManager.FILE_CONFIGS.getOrDefault(context.getTaskConfig().getTarget(), null);
        if (fileConfig == null) {
            context.getLogger().log("No file config found: %s", context.getTaskConfig().getTarget());
            current = Status.FAILURE_COMPLETE;
            return;
        }
        processTask(config, fileConfig, transform);

        if (current == Status.PROCESSING)
            current = Status.SUCCESS_COMPLETE;
    }

    private void processTask(RestQueryConfig config, FileConfig fileConfig, IntegrationTask.DelegateTransform transform) {
        context.getLogger().log("Fetching records from source");

        QueryExecutor executor  = new QueryExecutor(config, context.getRestQueryRequest(), context.getSourceDb());
        long          startTime = System.currentTimeMillis();
        List<Map<String, Object>> records = new ArrayList<>();

        try {
            String type = fileConfig.getFileType().toString().toLowerCase(Locale.ENGLISH);
            BaseResourceWriter writer = BaseResourceWriter.getResourceWriter(type, "pdf");
            if (fileConfig.getTarget() == FileConfig.StorageTarget.RESPONSE_STREAM)
                writer.init(context.getServletResponse(), config, fileConfig.getFileName());
            else if (fileConfig.getTarget() == FileConfig.StorageTarget.PERSIST_INTERNALLY) {
                BaseResourceWriter.FileStreamDescriptor descriptor = BaseResourceWriter.getFileStream();
                writer.init(descriptor.fileOutputStream, config, fileConfig.getFileName());
                context.getStagingFiles().put(fileConfig, descriptor.filePath);
            }
            List<Long> lastIndex = new ArrayList<>();
            lastIndex.add(0L);
            executor.selectWithCallback((record, index) -> {
                buildRecords(config, records, record);

                if ((index + 1) % 1000 == 0)
                    context.getLogger().log(String.format("Processing records %s, time elapsed in seconds %s", index + 1,
                            (System.currentTimeMillis() - startTime) / 1000));
                if ((index + 1) % context.getTaskConfig().getBulkCount() == 0) {
                    List<Map<String, Object>> processRecords = new ArrayList<>();
                    processRecords.addAll(records);
                    records.clear();

                    try {
                        writer.writeHeader(0, 0);
                        writer.writeContent(processRecords, 2, 0);
                    } catch (IOException ex1) {
                        log.error("Error while writing records to file:" + ex1.getLocalizedMessage());
                        ex1.printStackTrace();
                    }
                }
                lastIndex.set(0, index);
            });
            if (records.size() > 0) {
                try {
                    if ((lastIndex.get(0)+1) < context.getTaskConfig().getBulkCount()) {
                        writer.writeHeader(0, 0);
                        writer.writeContent(records, 2, 0);
                    }
                    else
                        writer.writeContent(records, (int)(lastIndex.get(0)+2), 0);
                } catch (IOException ex1) {
                    log.error("Error while writing records to file:" + ex1.getLocalizedMessage());
                    ex1.printStackTrace();
                }
            }
            writer.close();
        }
        catch (IOException ex2) {
            log.error("Exception while fetching records and opening file:"+ex2.getLocalizedMessage());
            ex2.printStackTrace();
        }
    }

    public String validateRestConfig(RestQueryConfig config) {
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML)
            return String.format("Import REST config can't be DML %s", config.getId());
        if (config.getFields().size() == 0)
            return String.format("No fields defined in the REST config %s", config.getId());
        DefaultEntityHandler handler = new DefaultEntityHandler();
        return handler.preValidateRestEntity(config, context.getRestQueryRequest());
    }
}
