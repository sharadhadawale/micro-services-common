package com.rajanainart.integration.task;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.mail.Mail;
import com.rajanainart.mail.MailConfig;
import com.rajanainart.mail.MailService;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.template.Template;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("integration-task-template_mail")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DbToTemplateMailTask extends ImportIntegrationTask {
    private static final Logger log = LoggerFactory.getLogger(DbToTemplateMailTask.class);

    @Override
    public IntegrationTask.Status process (IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String m = String.format("TEMPLATE_MAIL: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
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
        MailConfig mailConfig = IntegrationManager.MAIL_CONFIGS.getOrDefault(context.getTaskConfig().getTarget(), null);
        if (mailConfig == null) {
            context.getLogger().log("No mail config found: %s", context.getTaskConfig().getTarget());
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

        String mailTo  = context.getRestQueryRequest().getParams().getOrDefault("MailTo" , "");
        String subject = context.getRestQueryRequest().getParams().getOrDefault("Subject", "");
        if (mailTo.isEmpty() || subject.isEmpty()) {
            String msg = String.format("MailTo & Subject parameters are mandatory", context.getConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting DbToTemplateMail process");
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

        processTemplate(mailConfig, templates.get(0), result);

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

    private void processTemplate(MailConfig mailConfig, Map<String, Object> templateDb, List<? extends BaseEntity> records) {
        context.getLogger().log("Processing template %s", templateDb.get(Template.NAME));
        Template template = new Template(context.getLocalDb(), String.valueOf(templateDb.get(Template.NAME)),
                String.valueOf(templateDb.get(Template.DESCRIPTION)), String.valueOf(templateDb.get(Template.CONTENT)),
                "1 == 1", "");

        MailService mailService = AppContext.getBeanOfType(MailService.class);
        for (BaseEntity entity : records) {
            String mailContent = template.parse(entity);
            if (!mailContent.isEmpty()) {
                String log = String.format("Sending mail for the template:%s\r\nFinal Mail Content:%s", templateDb.get(Template.NAME), mailContent);
                context.getLogger().log(log);

                try {
                    Mail   mail    = Mail.getInstance(mailConfig);
                    String to      = context.getRestQueryRequest().getParams().get("MailTo");
                    String subject = context.getRestQueryRequest().getParams().get("Subject");
                    mail.setMailTo(MiscHelper.parseAsList(String.valueOf(to), ","));
                    mail.setMailBody(mailContent);
                    mail.setMailSubject(subject);
                    if (!mail.getMailFrom().isEmpty() && !mail.getMailTo().isEmpty())
                        mailService.sendEmail(mail);
                    else
                        context.getLogger().log("No valid from/to email id found, Template:%s", templateDb.get(Template.NAME));
                }
                catch (Exception ex) {
                    context.getLogger().log(String.format("Exception while sending email %s", ex.getLocalizedMessage()));
                    ex.printStackTrace();
                }
            }
        }
    }
}
