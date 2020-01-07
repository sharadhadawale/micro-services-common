package com.rajanainart.common.integration;

import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.data.nosql.BaseNoSqlDataProvider;
import com.rajanainart.common.integration.task.IntegrationTask;
import com.rajanainart.common.mq.MqMessageHandler;
import com.rajanainart.common.rest.RestQueryRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import java.io.Closeable;
import java.util.Map;

public class IntegrationContext implements Closeable {
    private Map<String, IntegrationTask      > tasks  = null;
    private Map<String, MqMessageHandler     > mqs    = null;
    private Map<String, BaseNoSqlDataProvider> nosqls = null;

    private IntegrationConfig config;
    private Database localDb  = null;
    private Database sourceDb = null;
    private Database targetDb = null;
    private String   task;
    private RestQueryRequest request    = null;
    private IntegrationLog      logger     = null;
    private Map<String, MultipartFile> uploadFiles = null;
    private HttpServletRequest servletRequest;

    public IntegrationConfig   getConfig          () { return config ; }
    public RestQueryRequest    getRestQueryRequest() { return request; }
    public IntegrationLog      getLogger          () { return logger ; }
    public String              getTask            () { return task   ; }
    public Map<String, MultipartFile> getUploadFiles() { return uploadFiles; }

    public Map<String, IntegrationTask      > getIntegrationTasks () { return tasks ; }
    public Map<String, MqMessageHandler     > getMqMessageHandlers() { return mqs   ; }
    public Map<String, BaseNoSqlDataProvider> getNoSqlDbProviders () { return nosqls; }

    public IntegrationContext(IntegrationConfig config, IntegrationLog logger, String task) {
        this.config  = config;
        this.task    = task  ;
        this.logger  = logger;
        this.request = new RestQueryRequest();

        this.tasks  = AppContext.getBeansOfType(IntegrationTask      .class);
        this.mqs    = AppContext.getBeansOfType(MqMessageHandler     .class);
        this.nosqls = AppContext.getBeansOfType(BaseNoSqlDataProvider.class);
    }

    public void setTask(String task) { this.task = task; }
    public void setRestQueryRequest(RestQueryRequest request) { this.request = request; }

    public HttpServletRequest getServletRequest() { return servletRequest; }

    public void setHttpServletRequest(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
        try {
            if (servletRequest != null) {
                MultipartHttpServletRequest mServletRequest = (MultipartHttpServletRequest)servletRequest;
                if (mServletRequest != null)
                    uploadFiles = mServletRequest.getFileMap();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Database getLocalDb() {
        if (localDb == null)
            localDb = new Database();

        return localDb;
    }

    public Database getSourceDb() {
        if (sourceDb == null) {
            IntegrationConfig.TaskConfig t = getTaskConfig();
            if (t.getSource().equalsIgnoreCase("local"))
                sourceDb = getLocalDb();
            else
                sourceDb = new Database(t.getSource());
        }
        return sourceDb;
    }

    public Database getTargetDb() {
        if (targetDb == null) {
            IntegrationConfig.TaskConfig t = getTaskConfig();
            if (t.getTarget().equalsIgnoreCase("local"))
                targetDb = getLocalDb();
            else
                targetDb = new Database(t.getTarget());
        }
        return targetDb;
    }

    public IntegrationConfig.TaskConfig getTaskConfig() {
        for (IntegrationConfig.TaskConfig t : config.getTasks())
            if (t.getId().equalsIgnoreCase(task)) return t;
        return null;
    }

    public IntegrationConfig.TaskConfig getTaskConfig(String taskId) {
        for (IntegrationConfig.TaskConfig t : config.getTasks())
            if (t.getId().equalsIgnoreCase(taskId)) return t;
        return null;
    }

    public void close() {
        if (localDb  != null) localDb .close();
        if (sourceDb != null) sourceDb.close();
        if (targetDb != null) targetDb.close();
    }
}
