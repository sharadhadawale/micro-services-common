package com.rajanainart.integration.task;

import com.rajanainart.data.QueryExecutor;
import com.rajanainart.integration.IntegrationContext;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.DefaultEntityHandler;

import com.rajanainart.rest.RestQueryConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component("integration-task-procedure")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DbProcIntegrationTask implements IntegrationTask {

    private IntegrationContext context = null;
    private Status             current = Status.PROCESSING;

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
        String msg = String.format("PROCEDURE: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(msg);
        RestQueryConfig config = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(context.getTaskConfig().getExecValue(), null);
        if (config == null || (config.getRestQueryType() == RestQueryConfig.RestQueryType.SELECT)) {
            context.getLogger().log("PROCEDURE REST query config can't be SELECT or null");
            current = Status.FAILURE_COMPLETE;
            return current;
        }
        DefaultEntityHandler handler = new DefaultEntityHandler();
        String result = handler.preValidateRestEntity(config, context.getRestQueryRequest());
        if (!result.equalsIgnoreCase(BaseRestController.SUCCESS)) {
            context.getLogger().log(result);
            current = Status.FAILURE_COMPLETE;
            return current;
        }
        QueryExecutor executor = new QueryExecutor(config, context.getRestQueryRequest(), context.getTargetDb());
        executor.executeQuery();
        context.getTargetDb().commit();

        current = Status.SUCCESS_COMPLETE;
        return current;
    }
}