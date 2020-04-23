package com.rajanainart.integration.task;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.resource.FileConfig;
import com.rajanainart.resource.pdf.PdfResourceWriter;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("integration-task-template_file")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DbToTemplateFileTask extends ImportIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DbToTemplateFileTask.class);

    @Override
    public IntegrationTask.Status process (IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String m = String.format("TEMPLATE_FILE: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
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

        String templateId = context.getRestQueryRequest().getParams().getOrDefault("TemplateId", "");
        if (!MiscHelper.isNumeric(templateId)) {
            String msg = String.format("Invalid 'TemplateId' request parameter", context.getConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting DbToTemplateFile process");
            current = Status.FAILURE_COMPLETE;
            return;
        }

        StringBuilder builder = new StringBuilder();
        builder.append("SELECT t.*\r\n")
                .append("FROM CMN_FILE_TEMPLATE t\r\n")
                .append("WHERE t.file_template_id = ?p_id\r\n");

        BaseEntity entity    = AppContext.getBeanOfType(BaseEntity.class, config.getEntityName());
        List<Map<String, Object>> templates = context.getSourceDb().selectAsMapList(builder.toString(),
                context.getSourceDb().new Parameter("p_id", templateId));

        if (templates.size() == 0) {
            context.getLogger().log("No template found for the TemplateId:%s", templateId);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        QueryExecutor executor = new QueryExecutor(config, context.getRestQueryRequest(), context.getSourceDb());
        List<? extends BaseEntity> result = null;

        if (entity.isJpaEntity())
            result = executor.findMultiple(entity.getClass());
        else
            result = executor.fetchResultSetAsEntity(entity.getClass());

        processTemplate(config, fileConfig, templates.get(0), result);

        if (current == Status.PROCESSING)
            current = Status.SUCCESS_COMPLETE;
    }

    @Override
    public String validateRestConfig(RestQueryConfig config) {
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML)
            return String.format("Import REST config can't be DML %s", config.getId());
        DefaultEntityHandler handler = new DefaultEntityHandler();
        return handler.preValidateRestEntity(config, context.getRestQueryRequest());
    }

    private void processTemplate(RestQueryConfig queryConfig, FileConfig fileConfig, Map<String, Object> templateDb, List<? extends BaseEntity> records) {
        context.getLogger().log("Processing template %s", templateDb.get(Template.NAME));
        Template template = new Template(context.getLocalDb(), String.valueOf(templateDb.get(Template.NAME)),
                                        String.valueOf(templateDb.get(Template.DESCRIPTION)), String.valueOf(templateDb.get(Template.CONTENT)),
                                        "1 == 1", "");
        String type = fileConfig.getFileType().toString().toLowerCase(Locale.ENGLISH);
        BaseResourceWriter writer = BaseResourceWriter.getResourceWriter(type, "pdf");
        boolean isPdf = false;
        int     index = 0;
        if (writer instanceof PdfResourceWriter)
            isPdf = true;

        try {
            if (fileConfig.getTarget() == FileConfig.StorageTarget.RESPONSE_STREAM)
                writer.init(context.getServletResponse(), queryConfig, fileConfig.getFileName());
            else if (fileConfig.getTarget() == FileConfig.StorageTarget.PERSIST_INTERNALLY) {
                BaseResourceWriter.FileStreamDescriptor descriptor = BaseResourceWriter.getFileStream();
                writer.init(descriptor.fileOutputStream, queryConfig, fileConfig.getFileName());
                context.getStagingFiles().put(fileConfig, descriptor.filePath);
            }
            StringBuilder builder = new StringBuilder();
            for (BaseEntity entity : records) {
                builder.append(template.parse(entity))
                       .append("\r\n");
                if (isPdf && ++index != records.size())
                    builder.append("<div style=\"page-break-after:always\">&nbsp;</div>\n");
            }
            try {
                if (records.size() != 0)
                    writer.writeHtml(builder.toString());
                else
                    writer.writeHtml("<p>No data found, please check the query used in rest-query-config</p>");
            } catch (IOException ex) {
                ex.printStackTrace();
            }
            writer.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
