package com.rajanainart.common.integration.task;

import com.rajanainart.common.integration.IntegrationContext;
import com.rajanainart.common.integration.IntegrationManager;
import com.rajanainart.common.nas.NasConfig;
import com.rajanainart.common.nas.NasFile;
import com.rajanainart.common.nas.NasFolder;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;

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
                NasFolder nasFolder = new NasFolder(nasConfig.getSource());
                nasFolder.setIntegrationLog(context.getLogger());
                nasFolder.copyAllFilesParallel(nasConfig.getTarget(), nasConfig.getFilterRegex(),
                                               nasConfig.getTargetExists() == NasConfig.TargetExists.REPLACE);
                current = Status.SUCCESS_COMPLETE;
            }
            else if (nasConfig.getOperationType() == NasConfig.OperationType.UPLOAD) {
                if (context.getUploadFile() != null) {
                    NasFile file = new NasFile(nasConfig.getTarget(), context.getUploadFile());
                    file.copy(null);
                    current = Status.SUCCESS_COMPLETE;
                }
                else {
                    context.getLogger().log("No file to upload to NAS");
                    current = Status.FAILURE_COMPLETE;
                }
            }
        }
        catch (IOException ex) {
            context.getLogger().log(String.format("Exception while copying to nas:%s", ex.getLocalizedMessage()));
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
