package com.rajanainart.resource;

import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.rest.RestQueryConfig;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Map;

public interface BaseResourceWriter extends Closeable {
    String FLAT_FILE_PREFIX = "file-generator";
    String FLAT_FILE_SUFFIX = ".tmp";

    void init(OutputStream outputStream, RestQueryConfig restQueryConfig, String fileName) throws IOException;
    void init(HttpServletResponse httpServletResponse, RestQueryConfig restQueryConfig, String fileName) throws IOException;
    void writeHeader (int startRow, int startCol) throws IOException;
    void writeContent(List<Map<String, Object>> records, int startRow, int startCol) throws IOException;
    void writeContent(String message, int startRow, int startCol) throws IOException;
    void writeHtml(String html) throws IOException;

    static BaseResourceWriter getResourceWriter(String type, String defaultType) {
        String key = String.format("resource-writer-%s", type);
        BaseResourceWriter writer = IntegrationManager.RESOURCE_WRITERS.getOrDefault(key, null);
        if (writer == null && (type.equalsIgnoreCase("csv") || type.equalsIgnoreCase("tsv")))
            writer = IntegrationManager.RESOURCE_WRITERS.get("resource-writer-txt");
        if (writer == null)
            writer = IntegrationManager.RESOURCE_WRITERS.get(String.format("resource-writer-%s", defaultType));
        return writer;
    }

    static FileStreamDescriptor getFileStream() throws IOException {
        File file = File.createTempFile(BaseResourceWriter.FLAT_FILE_PREFIX, BaseResourceWriter.FLAT_FILE_SUFFIX);
        FileOutputStream     stream     = new FileOutputStream(file, false);
        FileStreamDescriptor descriptor = new FileStreamDescriptor();
        descriptor.filePath         = file.getAbsolutePath();
        descriptor.fileOutputStream = stream;

        return descriptor;
    }

    class FileStreamDescriptor {
        public FileOutputStream fileOutputStream;
        public String filePath;
    }
}
