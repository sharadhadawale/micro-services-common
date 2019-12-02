package com.rajanainart.common.upload.task;

import com.rajanainart.common.upload.UploadContext;

public interface UploadTask {
    void executePre (UploadContext context);
    void executePost(UploadContext context);

    void executePreRecord (UploadContext context);
    void executePostRecord(UploadContext context);
    void executeRecord    (UploadContext context);
}
