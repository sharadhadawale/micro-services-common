package com.rajanainart.common.upload;

import com.rajanainart.common.data.Database;

import java.util.List;
import java.util.Map;

public class UploadContext {
    private Database     db;
    private UploadConfig config;
    private      Map<String, String > request;
    private List<Map<String, Object>> records;

    public Database     getUnderlyingDb() { return db    ; }
    public UploadConfig getUploadConfig() { return config; }

    public List<Map<String, Object>> getUploadRecords() { return records; }
    public Map     <String, String > getRequestParams() { return request; }

    public void setUploadRecords(List<Map<String, Object>> records) { this.records = records; }

    public UploadContext(Database db, UploadConfig config, Map<String, String> requestParams) {
        this.db     = db;
        this.config = config;
        this.request= requestParams;
    }
}
