package com.rajanainart.upload;

import com.rajanainart.data.Database;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class UploadContext {
    private Database db;
    private UploadConfig config;
    private Map<String, String> request;
    private List<Map<String, Object>> records = new ArrayList<>();
    private Map<String, Object> header;
    private int index = -1;

    public Database     getUnderlyingDb() { return db    ; }
    public UploadConfig getUploadConfig() { return config; }
    public int          getCurrentIndex() { return index ; }

    public List<Map<String, Object>> getUploadRecords() { return (records==null ? new ArrayList<>() : records) ;  }
    public Map<String, String> getRequestParams() { return request; }
    public Map<String, Object> getHeader       () { return header ; }

    void setUploadRecords(List<Map<String, Object>> records) { this.records = records; }
    void setHeader(Map<String, Object> header) { this.header = header; }
    void setCurrentIndex(int index) { this.index = index; }

    public UploadContext(Database db, UploadConfig config, Map<String, String> requestParams) {
        this.db     = db;
        this.config = config;
        this.request= requestParams;
    }
}
