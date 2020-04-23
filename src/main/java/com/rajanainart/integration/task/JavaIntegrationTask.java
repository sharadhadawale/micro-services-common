package com.rajanainart.integration.task;

import com.rajanainart.config.AppContext;
import com.rajanainart.integration.IntegrationContext;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("integration-task-java")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class JavaIntegrationTask implements IntegrationTask {
    public final static Map<String, BaseJavaIntegrationTask> JAVA_INTEGRATION_TASKS = AppContext.getBeansOfType(BaseJavaIntegrationTask.class);

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
        String msg = String.format("JAVA: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(msg);

        BaseJavaIntegrationTask task = JAVA_INTEGRATION_TASKS.getOrDefault(context.getTaskConfig().getExecValue(), null);
        if (task == null) {
            msg = String.format("Java integration task can't be found:%s", context.getTaskConfig().getExecValue());
            context.getLogger().log(msg);
            current = Status.WARNING_COMPLETE;
            return current;
        }

        task.setIntegrationContext(context);
        task.process(transform);

        current = task.getTaskStatus() != Status.PROCESSING ? task.getTaskStatus() : Status.SUCCESS_COMPLETE;
        return current;
    }
}
