package com.rajanainart.integration.task;

import com.rajanainart.integration.IntegrationContext;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.nas.NasConfig;
import com.rajanainart.nas.NasFile;
import com.rajanainart.nas.NasFolder;

import com.rajanainart.nas.NasSession;
import com.rajanainart.resource.FileConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Component("integration-task-nas")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NasIntegrationTask implements IntegrationTask {
    private IntegrationContext context   = null;
    private NasConfig          nasConfig = null;
    private Status             current   = Status.PROCESSING;

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
        String m = String.format("NAS: Processing exec %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
        context.getLogger().log(m);

        current = Status.PROCESSING;
        if (!validate()) return current;

        try {
            if (nasConfig.getOperationType() == NasConfig.OperationType.NAS_COPY) {
                try (NasSession sourceSession = new NasSession(nasConfig.getSource());
                     NasSession targetSession = new NasSession(nasConfig.getTarget())) {
                    NasFolder nasFolder = new NasFolder(sourceSession, "");
                    nasFolder.setIntegrationLog(context.getLogger());
                    nasFolder.copyAllFilesParallel(targetSession, "", nasConfig.getFilterRegex(),
                                           nasConfig.getTargetExists() == NasConfig.TargetExists.REPLACE);
                    current = Status.SUCCESS_COMPLETE;
                }
            }
            else if (nasConfig.getOperationType() == NasConfig.OperationType.UPLOAD) {
                if (context.getUploadFiles() != null) {
                    String subFolderName    = context.getServletRequest().getParameter(NasFile.HTTP_SUB_FOLDER_NAME_KEY);
                    try (NasSession session = new NasSession(nasConfig.getTarget())) {
                         NasFolder  folder  = new NasFolder(session, subFolderName, context.getUploadFiles());
                         folder.copyAllFilesParallel(session, subFolderName, false);
                         current = Status.SUCCESS_COMPLETE;
                    }
                }
                else {
                    context.getLogger().log("No file to upload to NAS");
                    current = Status.FAILURE_COMPLETE;
                }
            }
            else if (nasConfig.getOperationType() == NasConfig.OperationType.FILE_COPY) {
                FileConfig fileConfig = IntegrationManager.FILE_CONFIGS.getOrDefault(context.getTaskConfig().getSource(), null);
                if (fileConfig == null) {
                    String msg = String.format("Could not identify Source File Config of Nas Config %s/%s/%s", context.getConfig().getId(), context.getTaskConfig().getId(), context.getTaskConfig().getSource());
                    context.getLogger().log(msg);
                    current = Status.FAILURE_COMPLETE;
                    return current;
                }

                String stagingFile = context.getStagingFiles().getOrDefault(fileConfig, "");
                if (stagingFile.isEmpty()) {
                    context.getLogger().log("No staging file found:%s", stagingFile);
                    current = Status.FAILURE_COMPLETE;
                    return current;
                }

                File file = new File(stagingFile);
                try (InputStream stream = new FileInputStream(file)) {
                    try (NasSession session = new NasSession(nasConfig.getTarget())) {
                        NasFile nasFile = new NasFile(session, "", fileConfig.getFileName(), stream);
                        nasFile.copy(nasFile);
                        current = Status.SUCCESS_COMPLETE;
                    }
                }
            }
        }
        catch (IOException ex) {
            context.getLogger().log(String.format("Exception while copying to nas:%s", ex.getLocalizedMessage()));
            ex.printStackTrace();
            current = Status.FAILURE_COMPLETE;
        }
        return current;
    }

    private boolean validate() {
        nasConfig      = IntegrationManager.NAS_CONFIGS.getOrDefault(context.getTaskConfig().getExecValue(), null);
        if (nasConfig == null) {
            String msg = String.format("Could not identify Nas Config %s/%s", context.getConfig().getId(), context.getTaskConfig().getId());
            context.getLogger().log(msg);
            context.getLogger().log("Exiting nas process");
            current = Status.FAILURE_COMPLETE;
            return false;
        }
        return true;
    }
}
