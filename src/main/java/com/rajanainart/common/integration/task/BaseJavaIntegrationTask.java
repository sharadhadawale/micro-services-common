package com.rajanainart.common.integration.task;

import com.rajanainart.common.integration.IntegrationContext;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseJavaIntegrationTask {
    private IntegrationContext context;
    private IntegrationTask.Status status = IntegrationTask.Status.PROCESSING;
    private Map<String, String>    params = new HashMap<>();

    public void setTaskStatus        (IntegrationTask.Status status ) { this.status  = status ; }
    public void setIntegrationContext(IntegrationContext     context) { this.context = context; }

    public IntegrationTask.Status getTaskStatus        () { return status ; }
    public IntegrationContext     getIntegrationContext() { return context; }
    public Map<String, String>    getParams            () { return params ; }

    public abstract void process(IntegrationTask.DelegateTransform transform);
}
