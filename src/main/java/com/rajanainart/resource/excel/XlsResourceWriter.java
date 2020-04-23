package com.rajanainart.resource.excel;

import com.rajanainart.mail.MimeTypeConstant;
import com.rajanainart.resource.BaseResourceWriter;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.upload.ExcelDocument;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Component("resource-writer-xls")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XlsResourceWriter implements BaseResourceWriter {
    private RestQueryConfig restQueryConfig;
    private HttpServletResponse httpServletResponse;
    private ExcelDocument       excelDocument;
    private String fileName;

    @Override
    public void init(OutputStream outputStream, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig = restQueryConfig;
        this.fileName        = fileName;
        this.excelDocument   = new ExcelDocument(outputStream, fileName);
    }

    @Override
    public void init(HttpServletResponse httpServletResponse, RestQueryConfig restQueryConfig, String fileName) throws IOException {
        this.restQueryConfig     = restQueryConfig;
        this.httpServletResponse = httpServletResponse;
        this.fileName = fileName;

        init();
    }

    private void init() throws IOException {
        httpServletResponse.setContentType(MimeTypeConstant.getMimeType("xlsx"));
        httpServletResponse.setHeader("Content-Disposition", "attachment; filename="+fileName);

        excelDocument = new ExcelDocument(httpServletResponse.getOutputStream(), fileName);
        excelDocument.setSheetName(restQueryConfig.getName());
    }

    @Override
    public void writeHeader(int startRow, int startCol) throws IOException {
        excelDocument.writeString(restQueryConfig.getName(), startRow, startCol);

        Workbook workbook   = excelDocument.getWorkbook();
        Font     headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short)12);

        CellStyle headerCellStyle = workbook.createCellStyle();
        headerCellStyle.setFont(headerFont);
        excelDocument.getSheet().getRow(startRow).getCell(startCol).setCellStyle(headerCellStyle);
    }

    @Override
    public void writeContent(List<Map<String, Object>> records, int startRow, int startCol) throws IOException {
        excelDocument.writeRecords(restQueryConfig, records, startRow, startCol);
    }

    @Override
    public void writeContent(String message, int startRow, int startCol) throws IOException {
        excelDocument.writeString(message, startRow, startCol);
    }

    @Override
    public void writeHtml(String html) throws IOException {
        excelDocument.writeString(html, 0, 0);
    }

    @Override
    public void close() {
        try {
            excelDocument.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
