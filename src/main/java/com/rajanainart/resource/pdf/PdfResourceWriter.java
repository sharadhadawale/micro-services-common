package com.rajanainart.resource.pdf;

import com.rajanainart.mail.MimeTypeConstant;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.rest.RestQueryConfig;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Component("resource-writer-pdf")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PdfResourceWriter implements BaseResourceWriter {
    private RestQueryConfig restQueryConfig;
    private HttpServletResponse httpServletResponse;
    private PdfDocument pdfDocument;
    private String      fileName   ;

    @Override
    public void init(OutputStream outputStream, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig = restQueryConfig;
        this.fileName        = fileName;
        try {
            pdfDocument = new PdfDocument(outputStream);
        }
        catch (DocumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void init(HttpServletResponse httpServletResponse, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig     = restQueryConfig;
        this.httpServletResponse = httpServletResponse;
        this.fileName = fileName;

        init();
    }

    private void init() throws IOException {
        httpServletResponse.setContentType(MimeTypeConstant.getMimeType("pdf"));
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename="+fileName);

        try {
            pdfDocument = new PdfDocument(httpServletResponse.getOutputStream());
        }
        catch (DocumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void writeHeader(int startRow, int startCol) throws IOException {
        try {
            Font font = new Font(Font.FontFamily.HELVETICA, 15, Font.BOLD);
            pdfDocument.write(restQueryConfig.getName(), startRow+1, font);
        }
        catch (DocumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void writeContent(List<Map<String, Object>> records, int startRow, int startCol) throws IOException {
        try {
            Font header  = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD  );
            Font content = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            pdfDocument.write(restQueryConfig, records, startRow+1, header, content);
        }
        catch (DocumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void writeContent(String message, int startRow, int startCol) throws IOException {
        try {
            Font content = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
            pdfDocument.write(message, startRow+1, content);
        }
        catch (DocumentException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void writeHtml(String html) throws IOException {
        pdfDocument.writeHtml(html);
    }

    @Override
    public void close() throws IOException {
        pdfDocument.close();
    }
}
