package com.rajanainart.common.integration;

import com.rajanainart.common.config.AppConfig;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.nosql.NoSqlConfig;
import com.rajanainart.common.helper.MiscHelper;
import com.rajanainart.common.integration.iaas.IaaSRequest;
import com.rajanainart.common.integration.task.IntegrationTask;
import com.rajanainart.common.mq.MqConfig;
import com.rajanainart.common.nas.NasConfig;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.rest.RestQueryRequest;
import com.rajanainart.common.transform.BaseTransform;

import java.util.Map;

public class IntegrationManager implements Runnable {

    public final static Map<String, BaseTransform    > TRANSFORMS          = AppContext.getBeansOfType(BaseTransform.class);
    public final static Map<String, IntegrationConfig> INTEGRATION_CONFIGS = AppConfig.getBeansFromConfig("/integration-framework/process-config", "process-config", "id");
    public final static Map<String, NoSqlConfig      > NOSQL_CONFIGS       = AppConfig.getBeansFromConfig("/nosql-configs/nosql", "nosql-config", "id");
    public final static Map<String, MqConfig         > MQ_CONFIGS          = AppConfig.getBeansFromConfig("/mq-configs/mq"  , "mq-config" , "id");
    public final static Map<String, NasConfig        > NAS_CONFIGS         = AppConfig.getBeansFromConfig("/nas-configs/nas", "nas-config", "id");

    private IntegrationConfig  config  = null;
    private IntegrationContext context = null;
    private IntegrationLog     logger  = null;
    private RestQueryRequest   request = new RestQueryRequest();

    public IntegrationConfig  getConfig () { return config ; }
    public IntegrationContext getContext() { return context; }
    public IntegrationLog     getLogger () { return logger ; }

    public IntegrationManager(IaaSRequest iaaSRequest) {
        updateConfigWithIaaSRequest(iaaSRequest);

        init(iaaSRequest.getIntegrationName());
        context = new IntegrationContext(config, logger, iaaSRequest.getTaskName());

        synchronized (BaseRestController.REST_QUERY_CONFIGS) {
            String key = iaaSRequest.getRESTQueryName();
            BaseRestController.REST_QUERY_CONFIGS.get(key).buildFields(context.getSourceDb(), iaaSRequest);
        }
    }

    public IntegrationManager(String configId) {
        init(configId);
    }

    public IntegrationManager(String configId, RestQueryRequest request) {
        this(configId);
        if (request != null) {
            this.request = request;
            if (request.getParams() != null && logger != null && request.getParams().size() > 0)
                logger.log(MiscHelper.getMapAsString(request.getParams()));
        }
    }

    private void init(String configId) {
        if (!INTEGRATION_CONFIGS.containsKey(configId))
            throw new NullPointerException(String.format("Integration config %s does not exist", configId));
        config = INTEGRATION_CONFIGS.get(configId);
        logger = new IntegrationLog(config);
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
        IntegrationTask task   = null;
        IntegrationTask.Status status = IntegrationTask.Status.PROCESSING;
        for (IntegrationConfig.TaskConfig t : config.getTasks()) {
            try {
                if (t.getLevel() == IntegrationConfig.ExecLevel.DEPENDENT) continue;
                if (context == null)
                    context = new IntegrationContext(config, logger, t.getId());
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
        logger .close();
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
}
