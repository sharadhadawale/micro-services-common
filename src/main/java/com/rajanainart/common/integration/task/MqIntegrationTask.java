package com.rajanainart.common.integration.task;

import com.rajanainart.common.data.Database;
import com.rajanainart.common.data.QueryExecutor;
import com.rajanainart.common.data.encoder.JsonMessageEncoder;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.integration.IntegrationConfig;
import com.rajanainart.common.integration.IntegrationContext;
import com.rajanainart.common.integration.IntegrationManager;
import com.rajanainart.common.mq.MqConfig;
import com.rajanainart.common.mq.MqMessageHandler;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.DefaultEntityHandler;
import com.rajanainart.common.rest.RestQueryConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("integration-task-messaging")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MqIntegrationTask implements IntegrationTask {
    private static final Logger logger = LoggerFactory.getLogger(MqIntegrationTask.class);
    private IntegrationContext context  = null;
    private Status             current  = Status.PROCESSING;
    private MqConfig mqConfig = null;
    private IntegrationTask.DelegateTransform transform;

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
        String m = String.format("MESSAGING: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(m);

        current        = Status.PROCESSING;
        this.transform = transform;
        mqConfig       = IntegrationManager.MQ_CONFIGS.getOrDefault(context.getTaskConfig().getSource(), null);
        if (mqConfig == null) {
            String msg = String.format("Could not identify Mq Config %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting messaging process");
            current = Status.FAILURE_COMPLETE;
            return current;
        }

        String           key = mqConfig.getMqServerType().toString().toLowerCase();
        MqMessageHandler mq  = context.getMqMessageHandlers().getOrDefault(key, null);
        if (mq == null) {
            String msg = String.format("Could not identify Mq Message Handler %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
            context.getLogger().log(msg);
            current = Status.FAILURE_COMPLETE;
            return current;
        }

        mq.init(mqConfig);
        if (mqConfig.getMqUsedFor() == MqConfig.MqUsedFor.CONSUME) {
            String msg = String.format("Starting kafka poller Host:%s, Topic:%s, PollDurationInMns: %s",
                                            mqConfig.getHost(), mqConfig.getTopicName(), mqConfig.getDurationMns());
            context.getLogger().log(msg);
            initRuntimeHook();
            mq.consume(this::processData);
        }
        else if (mqConfig.getMqUsedFor() == MqConfig.MqUsedFor.PUBLISH) {
            Map<String, Object> properties = new HashMap<>();
            int index = 0;
            for (String ct : context.getTaskConfig().getExecValues()) {
                IntegrationConfig.TaskConfig task = context.getTaskConfig(ct);
                RestQueryConfig queryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(task.getExecValue(), null);
                if (queryConfig == null) {
                    String msg = "Publish can't be completed, as the REST configuration is not available";
                    context.getLogger().log(msg);
                    current = Status.FAILURE_COMPLETE;
                    return current;
                }
                DefaultEntityHandler handler = new DefaultEntityHandler();
                String result = handler.preValidateRestEntity(queryConfig, context.getRestQueryRequest());
                if (!result.equalsIgnoreCase(BaseRestController.SUCCESS)) {
                    context.getLogger().log(result);
                    current = Status.FAILURE_COMPLETE;
                    return current;
                }

                if (queryConfig.getRestQueryUsedFor() == RestQueryConfig.RestQueryUsedFor.META)
                    buildMqMetaMessage(properties, index++, queryConfig, task);
                else if (queryConfig.getRestQueryUsedFor() == RestQueryConfig.RestQueryUsedFor.RESULT) {
                    QueryExecutor executor = new QueryExecutor(queryConfig, context.getRestQueryRequest(), context.getSourceDb());
                    List<Map<String, Object>> records  = executor.fetchResultSet();

                    String  messageId = String.format("integration-result-%s-%s", context.getConfig().getId(), new SecureRandom().nextLong());
                    boolean success   = mq.publish(queryConfig.getResponseContentType().toString().toLowerCase(), messageId, records);
                    if (success) {
                        String msg = String.format("Kafka message has been published, Host:%s, Topic:%s, RestConfig:%s",
                                                   mqConfig.getHost(), mqConfig.getTopicName(), queryConfig.getId());
                        context.getLogger().log(msg);
                    }
                }
            }
            if (properties.size() > 0) {
                String messageId = String.format("integration-meta-for-spark-%s-%s", context.getConfig().getId(), new SecureRandom().nextLong());
                boolean success  = mq.publish(JsonMessageEncoder.NAME, messageId, properties);
                if (success) {
                    String msg = String.format("Kafka message has been published, Host:%s, Topic:%s, TaskId:%s",
                                                mqConfig.getHost(), mqConfig.getTopicName(), context.getTaskConfig().getId());
                    context.getLogger().log(msg);
                }
            }
            try {
                mq.close();
            }
            catch(Exception e) {
                logger.error("Error while Mq close");
                e.printStackTrace();
            }
        }
        current = Status.SUCCESS_COMPLETE;
        return current;
    }

    private void initRuntimeHook() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                String msg = String.format("Shutting down kafka poller Host:%s, Topic:%s", mqConfig.getHost(), mqConfig.getTopicName());
                logger.info(msg);

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
                    logger.error("Exception while shutting down kafka poller");
                    ex.printStackTrace();
                }
            }
        });
    }

    private void buildMqMetaMessage(Map<String, Object> properties, int idx,
                                    RestQueryConfig queryConfig, IntegrationConfig.TaskConfig task) {
        properties.put("process_id", context.getLogger().getProcessId());
        if (MiscHelper.isValidName(queryConfig.getId()))
            properties.put(String.format("%s-id"    , idx), HtmlUtils.htmlEscape(queryConfig.getId()));
        if (MiscHelper.isValidName(task.getSource()))
            properties.put(String.format("%s-source", idx), HtmlUtils.htmlEscape(task.getSource()));
        if (MiscHelper.isValidName(task.getTarget()))
            properties.put(String.format("%s-target", idx), HtmlUtils.htmlEscape(task.getTarget()));
        if (MiscHelper.isValidName(queryConfig.getTarget()))
            properties.put(String.format("%s-target-table", idx), HtmlUtils.htmlEscape(queryConfig.getTarget()));
        QueryExecutor executor = new QueryExecutor(queryConfig, context.getRestQueryRequest(), context.getLogger().getLoggerDb());
        String query           = queryConfig.getQuery();
        for (Database.Parameter p : executor.buildDbParameters()) {
            query = query.replaceAll(String.format("%s%s", Database.QUERY_PARAMETER_REGEX, HtmlUtils.htmlEscape(p.getName())),
                                     String.format("'%s'", p.getValue()));
            if (MiscHelper.isValidName(p.getName()))
                properties.put(String.format("%s-parameter-%s", idx, HtmlUtils.htmlEscape(p.getName())), p.getValue());
        }
        properties.put(String.format("%s-query", idx), query);

        int index = 0;
        for (String pre : task.getPreExecValue()) {
            RestQueryConfig preConf = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(pre, null);
            if (preConf != null) {
                QueryExecutor executor1 = new QueryExecutor(preConf, context.getRestQueryRequest(), context.getLogger().getLoggerDb());
                query = executor1.buildQueryWithoutParameters();
                properties.put(String.format("%s-pre-query-%s", idx, index++), query);
            }
        }

        index = 0;
        for (String post : task.getPostExecValue()) {
            RestQueryConfig postConf = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(post, null);
            if (postConf != null) {
                QueryExecutor executor1 = new QueryExecutor(postConf, context.getRestQueryRequest(), context.getLogger().getLoggerDb());
                query = executor1.buildQueryWithoutParameters();
                properties.put(String.format("%s-post-query-%s", idx, index++), query);
            }
        }

        for (RestQueryConfig.FieldConfig f : queryConfig.getFields())
            properties.put(String.format("%s-field-%s", idx, f.getId()), f);
    }

    public void processData(Object result) {
        if (mqConfig.getMqServerType() == MqConfig.MqServerType.KAFKA) {
            ConsumerRecords<String, String> records = (ConsumerRecords<String, String>)result;
            records.forEach(record -> {
                String log = String.format("Kafka Message received, Timestamp:%s, Key:%s, Topic:%s, Partition:%s, Offset:%s",
                                    record.timestamp(), record.key(), record.topic(), record.partition(), record.offset());
                logger.info(String.format("Value:%s", record.value()));
                context.getLogger().log(log);
                log = String.format("MQ: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
                context.getLogger().log(log);

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
                            context.getLogger().log("Exception while executing dependent task on a MqMessage arrival");
                            ex.printStackTrace();
                        }
                        finally {
                            newContext.close();
                        }
                    }
                }
            });
        }
    }
}
