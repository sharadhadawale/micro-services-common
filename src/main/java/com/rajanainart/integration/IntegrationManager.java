package com.rajanainart.integration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.rajanainart.config.AppConfig;
import com.rajanainart.config.AppContext;
import com.rajanainart.data.Database;
import com.rajanainart.data.nosql.NoSqlConfig;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.integration.iaas.IaaSRequest;
import com.rajanainart.integration.task.IntegrationTask;
import com.rajanainart.mail.MailConfig;
import com.rajanainart.mq.MqConfig;
import com.rajanainart.nas.NasConfig;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.resource.FileConfig;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.rest.RestQueryRequest;
import com.rajanainart.transform.BaseTransform;

public class IntegrationManager implements Runnable {

    public final static Map<String, BaseTransform     > TRANSFORMS         = AppContext.getBeansOfType(BaseTransform.class);
    public final static Map<String, BaseResourceWriter> RESOURCE_WRITERS   = AppContext.getBeansOfType(BaseResourceWriter.class);
    public final static Map<String, IntegrationConfig> INTEGRATION_CONFIGS = AppConfig.getBeansFromConfig("/integration-framework/process-config", "process-config", "id");
    public final static Map<String, NoSqlConfig      > NOSQL_CONFIGS       = AppConfig.getBeansFromConfig("/nosql-configs/nosql", "nosql-config", "id");
    public final static Map<String, MqConfig> MQ_CONFIGS          = AppConfig.getBeansFromConfig("/mq-configs/mq"      , "mq-config"   , "id");
    public final static Map<String, NasConfig        > NAS_CONFIGS         = AppConfig.getBeansFromConfig("/nas-configs/nas"    , "nas-config"  , "id");
    public final static Map<String, MailConfig       > MAIL_CONFIGS        = AppConfig.getBeansFromConfig("/mail-configs/mail"  , "mail-config" , "id");
    public final static Map<String, FileConfig       > FILE_CONFIGS        = AppConfig.getBeansFromConfig("/file-configs/file", "file-config", "id");

    private IntegrationTask.Status status = IntegrationTask.Status.PROCESSING;
    private IntegrationConfig  config  = null;
    private IntegrationContext context = null;
    private IntegrationLog     logger  = null;
    private HttpServletRequest  servletRequest ;
    private HttpServletResponse servletResponse;
    private RestQueryRequest   request = new RestQueryRequest();

    public IntegrationConfig  getConfig () { return config ; }
    public IntegrationContext getContext() { return context; }
    public IntegrationLog     getLogger () { return logger ; }
    public IntegrationTask.Status getLastStatus() { return status; }

    public IntegrationManager(HttpServletRequest servletRequest, HttpServletResponse servletResponse, IaaSRequest iaaSRequest) {
        updateConfigWithIaaSRequest(iaaSRequest);

        init(servletRequest, servletResponse, iaaSRequest.getIntegrationName());
        context = new IntegrationContext(config, logger, iaaSRequest.getTaskName());
        context.setHttpServletRequest(servletRequest);

        synchronized (BaseRestController.REST_QUERY_CONFIGS) {
            String key = iaaSRequest.getRESTQueryName();
            BaseRestController.REST_QUERY_CONFIGS.get(key).buildFields(context.getSourceDb(), iaaSRequest);
        }
    }

    public IntegrationManager(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String configId) {
        init(servletRequest, servletResponse, configId);
    }

    public IntegrationManager(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String configId, RestQueryRequest request) {
        this(servletRequest, servletResponse, configId);
        if (request != null) {
            this.request = request;
            if (request.getParams() != null && logger != null && request.getParams().size() > 0)
                logger.log(MiscHelper.getMapAsString(request.getParams()));
        }
    }

    private void init(HttpServletRequest servletRequest, HttpServletResponse servletResponse, String configId) {
        if (!INTEGRATION_CONFIGS.containsKey(configId))
            throw new NullPointerException(String.format("Integration config %s does not exist", configId));
        config = INTEGRATION_CONFIGS.get(configId);
        logger = new IntegrationLog(config);
        this.servletRequest  = servletRequest ;
        this.servletResponse = servletResponse;
        logger.log(String.format("Integration process is initialized with config %s", config.getId()));
    }

    private void updateConfigWithIaaSRequest(IaaSRequest iaaSRequest) {
        IntegrationConfig integrationConfig = IntegrationConfig.getInstance(iaaSRequest);
        synchronized (INTEGRATION_CONFIGS) {
            String key = iaaSRequest.getIntegrationName();
            INTEGRATION_CONFIGS.put(key, integrationConfig);
        }

        RestQueryConfig queryConfig = RestQueryConfig.getInstance(iaaSRequest);
        synchronized (BaseRestController.REST_QUERY_CONFIGS) {
            String key = iaaSRequest.getRESTQueryName();
            BaseRestController.REST_QUERY_CONFIGS.put(key, queryConfig);
        }

        if (iaaSRequest.getTaskType() == IntegrationConfig.TaskType.NOSQL_IMPORT) {
            synchronized (NOSQL_CONFIGS) {
                String key = iaaSRequest.getNoSqlConfigName();
                NOSQL_CONFIGS.put(key, NoSqlConfig.getInstance(iaaSRequest));
            }
        }
    }

    @Override
    public void run() {
        IntegrationTask task = null;
        for (IntegrationConfig.TaskConfig t : config.getTasks()) {
            try {
                if (t.getLevel() == IntegrationConfig.ExecLevel.DEPENDENT) continue;
                if (context == null) {
                    context = new IntegrationContext(config, logger, t.getId());
                    context.setHttpServletRequest (servletRequest );
                    context.setHttpServletResponse(servletResponse);
                }
                else
                    context.setTask(t.getId());
                context.setRestQueryRequest(request);

                String key = String.format("integration-task-%s", t.getType().name().toLowerCase());
                task       = context.getIntegrationTasks().getOrDefault(key, null);
                if (task  == null) {
                    String msg = String.format("Could not identify the task for the configuration integration/task:%s/%s", config.getId(), t.getId());
                    context.getLogger().log(msg);
                    continue;
                }

                task.setup(context);
                task.process(this::transform);
                String msg = String.format("Completing the task %s", t.getId());
                context.getLogger().log(msg);

                status = task.currentStatus();
                if (status == IntegrationTask.Status.FAILURE_COMPLETE) {
                    context.getLogger().log("Completing the integration process with error");
                    break;
                }
            }
            catch(Exception ex) {
                status = IntegrationTask.Status.FAILURE_COMPLETE;
                if (context != null)
                    context.getLogger().log(String.format("Unexpected exception has occurred while processing integration tasks %s", ex.getMessage()));
                ex.printStackTrace();
                break;
            }
        }
        if (context != null)
            context.getLogger().log("Completed integration process");
        logger.completeProcess(status);
        logger.close();
        if (context != null)
            context.close();
    }

    public void transform(IntegrationContext newContext, Map<String, Object> result) {
        for (IntegrationConfig.TransformConfig t : newContext.getTaskConfig().getTransforms()) {
            String key;
            if (t.getTransformType() != IntegrationConfig.TransformType.CUSTOM)
                key = String.format("transform-%s", t.getTransformType().toString().toLowerCase());
            else
                key = String.format("transform-%s-%s", t.getTransformType().toString().toLowerCase(), newContext.getConfig().getId());
            BaseTransform transform = IntegrationManager.TRANSFORMS.getOrDefault(key, null);
            if (transform != null) {
                Object r = transform.transform(newContext.getTargetDb(), result.get(t.getField()), t.getParams());
                if (r != null)
                    result.put(t.getField(), r);
            }
        }
    }

    public static List<IntegrationConfig> getIntegrationConfigs(IntegrationConfig.TaskType type) {
        List<IntegrationConfig> result = new ArrayList<>();
        for(Map.Entry<String, IntegrationConfig> config : IntegrationManager.INTEGRATION_CONFIGS.entrySet()) {
            if (!config.getValue().getActive()) continue;

            for (IntegrationConfig.TaskConfig task : config.getValue().getTasks()) {
                if (task.getType() == type) {
                    result.add(config.getValue());
                    break;
                }
            }
        }
        return result;
    }

    public static void closeProcess(Database db, String configName) {
        Long processId = db.selectScalar("SELECT process_id AS status FROM CMN_INTEGRATION_PROCESS WHERE status = 0 AND config_name = ?p_name1",
                db.new Parameter("p_name1", configName));
        if (processId != null) {
            db.executeQueryWithJdbc("INSERT INTO CMN_INTEGRATION_PROCESS_LOG (process_id, log, as_on) VALUES (?p_id, 'Forced shutdown', CURRENT_TIMESTAMP)",
                    db.new Parameter("p_id"  , processId));
            db.executeQueryWithJdbc("UPDATE CMN_INTEGRATION_PROCESS SET status = 1 WHERE process_id = ?p_id AND config_name = ?p_name",
                    db.new Parameter("p_id"  , processId),
                    db.new Parameter("p_name", configName));
            db.commit();
        }
    }
}
