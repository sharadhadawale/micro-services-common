package com.rajanainart.common.integration.task;

import com.rajanainart.common.concurrency.ConcurrencyManager;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.QueryExecutor;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.integration.IntegrationManager;
import com.rajanainart.common.mail.Mail;
import com.rajanainart.common.mail.MailConfig;
import com.rajanainart.common.mail.MailService;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.DefaultEntityHandler;
import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.template.Template;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("integration-task-mail")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MailIntegrationTask extends ImportIntegrationTask {
    @Override
    public Status process(IntegrationTask.DelegateTransform transform) {
        current = Status.PROCESSING;
        String msg = String.format("MAIL: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(msg);
        processRestQuery(context.getTaskConfig().getExecValue(), transform);
        return current;
    }

    public void processRestQuery(String query, IntegrationTask.DelegateTransform transform) {
        RestQueryConfig config = BaseRestController.REST_QUERY_CONFIGS.get(query);
        String          valid  = validateRestConfig(config);
        if (!valid.equalsIgnoreCase(BaseRestController.SUCCESS)) {
            context.getLogger().log(valid);
            current = Status.FAILURE_COMPLETE;
            return;
        }

        BaseEntity                entity    = AppContext.getBeanOfType(BaseEntity.class, config.getEntityName());
        List<Map<String, Object>> templates = context.getSourceDb().selectAsMapList("SELECT * FROM CMN_TEMPLATE");
        QueryExecutor              executor = new QueryExecutor(config, context.getRestQueryRequest(), context.getSourceDb());
        List<? extends BaseEntity> result   = null;

        if (entity.isJpaEntity())
            result = executor.findMultiple(entity.getClass());
        else
            result = executor.fetchResultSetAsEntity(entity.getClass());

        try (ConcurrencyManager concurrency = new ConcurrencyManager("integration-mail-task")) {
            for (BaseEntity record : result) {
                List<BaseEntity> input = new ArrayList<>();
                input.add(record);
                for (Map<String, Object> t : templates)
                    concurrency.submit(new MailThread(t, input));
            }
            concurrency.awaitTermination((message -> context.getLogger().log(message)), 100);
        }
        if (current == Status.PROCESSING)
            current = Status.SUCCESS_COMPLETE;
    }

    public String validateRestConfig(RestQueryConfig config) {
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML)
            return String.format("Import REST config can't be DML %s", config.getId());

        BaseEntity entity = AppContext.getBeanOfType(BaseEntity.class, config.getEntityName());
        if (entity == null)
            return String.format("BaseEntity does not exist:%s", config.getEntityName());

        MailConfig mailConfig = IntegrationManager.MAIL_CONFIGS.getOrDefault(context.getTaskConfig().getTarget(), null);
        if (mailConfig == null)
            return String.format("MailConfig does not exist:%s", context.getTaskConfig().getTarget());

        DefaultEntityHandler handler = new DefaultEntityHandler();
        return handler.preValidateRestEntity(config, context.getRestQueryRequest());
    }

    public class MailThread implements ConcurrencyManager.BaseConcurrencyThread {
        private boolean  complete = false;
        private String   name     = "";

        private List<BaseEntity>    input;
        private Map<String, Object> templateDb;

        public boolean getIsComplete() { return complete; }
        public String  getThreadName() { return name    ; }

        public MailThread(Map<String, Object> templateDb, List<BaseEntity> input) {
            this.templateDb = templateDb;
            this.input      = input;
        }

        @Override
        public void run() {
            name = String.format("%s-%s", Thread.currentThread().getName(), new SecureRandom().nextLong());

            MailConfig  mailConfig  = IntegrationManager.MAIL_CONFIGS.get(context.getTaskConfig().getTarget());
            MailService mailService = AppContext.getBeanOfType(MailService.class);
            Template template       = new Template(context.getLocalDb(),
                                                   String.valueOf(templateDb.get(Template.NAME   )), String.valueOf(templateDb.get(Template.DESCRIPTION)),
                                                   String.valueOf(templateDb.get(Template.CONTENT)), String.valueOf(templateDb.get(Template.CONDITION  )),
                                                   String.valueOf(templateDb.get(Template.MAIL_TO)));
            String mailContent = template.parse(input);
            if (!mailContent.isEmpty()) {
                try {
                    Mail mail = Mail.getInstance(mailConfig);
                    Object to = input.get(0).getValue(Template.MAIL_TO);
                    if (to == null) to = templateDb.getOrDefault(Template.MAIL_TO, null);
                    if (to != null) mail.setMailTo(MiscHelper.parseAsList(String.valueOf(to), ","));
                    mail.setMailBody(mailContent);
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
            complete = true;
        }
    }
}
