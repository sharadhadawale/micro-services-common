package com.rajanainart.resource.flatfile;

import com.rajanainart.resource.FileConfig;
import com.rajanainart.rest.RestQueryConfig;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.List;
import java.util.Map;

public class FlatFile implements Closeable {
    private FileConfig      fileConfig;
    private RestQueryConfig restConfig;
    private boolean isFirstBatch = true;

    private File          file   ;
    private OutputStream  stream ;
    private FileChannel   channel;
    private StringBuilder builder;

    public  FileConfig      getFileConfig() { return fileConfig    ; }
    public  RestQueryConfig getRestConfig() { return restConfig    ; }
    public  String          getFilePath  () { return file.getAbsolutePath(); }

    public FlatFile(FileConfig fileConfig, RestQueryConfig restConfig, OutputStream outputStream) throws IOException {
        this.fileConfig = fileConfig;
        this.restConfig = restConfig;
        this.stream     = outputStream;

        builder = new StringBuilder();
        if (outputStream instanceof FileOutputStream)
            channel = ((FileOutputStream)outputStream).getChannel();
    }

    public void write(String message) throws IOException {
        if (channel != null)
            channel.write(ByteBuffer.wrap(message.getBytes()));
        else if (stream != null)
            stream.write(message.getBytes());
    }

    public void write(List<Map<String, Object>> records) throws IOException {
        if (records.size() == 0) return;

        if (isFirstBatch && fileConfig.hasHeader()) {
            writeHeader(records.get(0));
            isFirstBatch = false;
        }

        for (Map<String, Object> record : records) {
            int index = 0;
            if (restConfig.getFields().size() > 0) {
                for (RestQueryConfig.FieldConfig field : restConfig.getFields()) {
                    if (!field.getIsVisible()) continue;

                    builder.append(String.format("%s\"%s\"", index++ != 0 ? fileConfig.getDelimiter() : "", record.get(field.getId())));
                }
            }
            else {
                for (String key : record.keySet())
                    builder.append(String.format("%s\"%s\"", index++ != 0 ? fileConfig.getDelimiter() : "", record.get(key)));
            }
            builder.append("\r\n");
        }
        if (channel != null)
            channel.write(ByteBuffer.wrap(builder.toString().getBytes()));
        else if (stream != null)
            stream.write(builder.toString().getBytes());
    }

    private void writeHeader(Map<String, Object> oneRecord) {
        int index = 0;
        if (restConfig.getFields().size() > 0) {
            for (RestQueryConfig.FieldConfig field : restConfig.getFields()) {
                if (!field.getIsVisible()) continue;

                builder.append(String.format("%s\"%s\"", index++ != 0 ? fileConfig.getDelimiter() : "", field.getName()));
            }
        }
        else {
            for (String key : oneRecord.keySet())
                builder.append(String.format("%s\"%s\"", index++ != 0 ? fileConfig.getDelimiter() : "", key));
        }
        builder.append("\r\n");
    }

    @Override
    public void close() throws IOException {
        if (channel != null) channel.close();
        if (stream  != null) stream .close();
    }
}
