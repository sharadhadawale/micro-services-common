package com.rajanainart.upload.task;

import com.rajanainart.upload.UploadConfig;
import com.rajanainart.upload.UploadContext;

import java.util.List;

public interface UploadTask {
    void executePre (UploadContext context);
    void executePost(UploadContext context);

    void executePreRecord (UploadContext context);
    void executePostRecord(UploadContext context);
    void executeRecord    (UploadContext context);

    void onDownloadFormat(UploadConfig config, List<String> additionalColumns);
}
