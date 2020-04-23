package com.rajanainart.rest;

import org.apache.commons.fileupload.FileItem;
import org.apache.poi.util.IOUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ServletRequestWrapper extends HttpServletRequestWrapper implements Closeable {

    private byte[] rawData;
    private HttpServletRequest        request      ;
    private ServletInputStreamWrapper servletStream;

    public ServletRequestWrapper(HttpServletRequest request) throws IOException, ServletException {
        super(request);
        this.request       = request;
        this.servletStream = new ServletInputStreamWrapper();
        getInputStream();
        getFileMap();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getInputStream());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }
        return servletStream;
    }

    private Map<String, MultipartFile> fileMap = null;
    public Map<String, MultipartFile> getFileMap() throws IOException, ServletException {
        if (fileMap != null) return fileMap;

        fileMap = new HashMap<>();
        Collection<Part> parts = super.getParts();
        for (Part part : parts) {
            FileItem      fileItem      = new LocalFileItem(IOUtils.toByteArray(this.request.getInputStream()));
            MultipartFile multipartFile = new CommonsMultipartFile(fileItem);
            fileMap.put(part.getName(), multipartFile);
        }
        return fileMap;
    }

    public void resetInputStream() {
        servletStream.stream = new ByteArrayInputStream(rawData);
    }

    @Override
    public BufferedReader getReader() throws IOException {
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getInputStream());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }
        return new BufferedReader(new InputStreamReader(servletStream));
    }

    public class ServletInputStreamWrapper extends ServletInputStream {
        private InputStream stream;

        @Override public boolean isFinished() { return false; }
        @Override public boolean isReady   () { return false; }

        @Override public void setReadListener(ReadListener listener) { }
        @Override public int read() throws IOException { return stream.read(); }
    }

    @Override
    public void close() {
    }
}

