package com.rajanainart.common.rest;

import org.apache.poi.util.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;

public class ServletRequestWrapper extends HttpServletRequestWrapper {

    private byte[] rawData;
    private HttpServletRequest        request      ;
    private ServletInputStreamWrapper servletStream;

    public ServletRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request       = request;
        this.servletStream = new ServletInputStreamWrapper();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (rawData == null) {
            rawData = IOUtils.toByteArray(this.request.getInputStream());
            servletStream.stream = new ByteArrayInputStream(rawData);
        }
        return servletStream;
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
}

