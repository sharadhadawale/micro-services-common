package com.rajanainart.rest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemHeaders;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class LocalFileItem implements FileItem, Closeable {
    private static final long serialVersionUID = 2467880290855097332L;

    private File   file ;
    private byte[] bytes;

    public LocalFileItem(byte[] bytes) throws IOException {
        this.file  = File.createTempFile("local-file-item", ".tmp");
        this.bytes = bytes;

        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(bytes);
        }
    }

    @Override
    public void write(File target) throws Exception {
        Files.move(file.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public long getSize() {
        long size = -1L;
        try {
            size = Files.size(file.toPath());
        } catch (IOException ignored) {}
        return size;
    }

    @Override
    public void delete() {
        file.delete();
    }

    private FileItemHeaders headers;
    private String contentType;
    private String fieldName;
    private boolean formField;

    @Override
    public FileItemHeaders getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(FileItemHeaders headers) {
        this.headers = headers;
    }

    private InputStream inputStream = null;
    @Override
    public InputStream getInputStream() throws IOException {
        if (inputStream == null)
            inputStream = new ByteArrayInputStream(bytes);
        return inputStream;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isInMemory() {
        return false;
    }

    @Override
    public byte[] get() {
        return bytes;
    }

    @Override
    public String getString(String encoding) throws UnsupportedEncodingException {
        throw new RuntimeException("Only method write(File) is supported.");
    }

    @Override
    public String getString() {
        throw new RuntimeException("Only method write(File) is supported.");
    }

    @Override
    public String getFieldName() {
        return fieldName;
    }

    @Override
    public void setFieldName(String name) {
        this.fieldName = name;
    }

    @Override
    public boolean isFormField() {
        return formField;
    }

    @Override
    public void setFormField(boolean state) {
        this.formField = state;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("Not supported");
    }

    @Override
    public void close() throws IOException {
        if (inputStream != null) inputStream.close();
        delete();
    }
}

