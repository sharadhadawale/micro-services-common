package com.rajanainart.resource.flatfile;

import com.rajanainart.mail.MimeTypeConstant;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.resource.FileConfig;
import com.rajanainart.rest.RestQueryConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component("resource-writer-txt")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class TxtResourceWriter implements BaseResourceWriter {
    private RestQueryConfig restQueryConfig;
    private HttpServletResponse httpServletResponse;
    private FlatFile flatFile;
    private String   fileName;

    @Override
    public void init(OutputStream outputStream, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig = restQueryConfig;
        this.fileName        = fileName;
        this.flatFile        = new FlatFile(FileConfig.getInstance(fileName, FileConfig.StorageTarget.PERSIST_INTERNALLY),
                                            restQueryConfig, outputStream);
    }

    @Override
    public void init(HttpServletResponse httpServletResponse, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig     = restQueryConfig;
        this.httpServletResponse = httpServletResponse;
        this.fileName = fileName;

        init();
    }

    private void init() throws IOException {
        this.flatFile = new FlatFile(FileConfig.getInstance(fileName, FileConfig.StorageTarget.RESPONSE_STREAM),
                                     restQueryConfig, httpServletResponse.getOutputStream());
        String type   = flatFile.getFileConfig().getFileType().toString().toLowerCase(Locale.ENGLISH);
        httpServletResponse.setContentType(MimeTypeConstant.getMimeType(type));
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename="+fileName);
    }

    @Override
    public void writeHeader(int startRow, int startCol) throws IOException {
        flatFile.write(restQueryConfig.getName()+"\r\n");
    }

    @Override
    public void writeContent(List<Map<String, Object>> records, int startRow, int startCol) throws IOException {
        flatFile.write(records);
    }

    @Override
    public void writeContent(String message, int startRow, int startCol) throws IOException {
        flatFile.write(message);
    }

    @Override
    public void writeHtml(String html) throws IOException {
        writeContent(html, 0, 0);
    }

    @Override
    public void close() throws IOException {
        flatFile.close();
    }
}
