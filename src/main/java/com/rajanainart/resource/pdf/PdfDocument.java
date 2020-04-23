package com.rajanainart.resource.pdf;

import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.rest.RestQueryConfig;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.List;
import java.util.Map;

public class PdfDocument implements Closeable {
    private Document     document;
    private OutputStream outputStream;
    private PdfWriter    pdfWriter;
    private XMLWorkerHelper xmlWorkerHelper;
    private int headerLine = 0;

    public PdfDocument(OutputStream outputStream) throws DocumentException {
        this.document        = new Document();
        this.outputStream    = outputStream;
        this.pdfWriter       = PdfWriter.getInstance(document, outputStream);
        this.xmlWorkerHelper = XMLWorkerHelper.getInstance();
        document.open();
    }

    public static void addEmpty(Paragraph paragraph, int length) {
        for (int index = 0; index < length; index++)
            paragraph.add(new Paragraph(" "));
    }

    public void write(String message, int startLine, Font font) throws DocumentException {
        headerLine = startLine;
        Paragraph paragraph = new Paragraph();
        addEmpty(paragraph, startLine);
        paragraph.add(new Paragraph(message, font));

        document.add(paragraph);
    }

    public void writeHtml(String html) throws IOException {
        xmlWorkerHelper.parseXHtml(pdfWriter, document, new StringReader(html));
    }

    public void write(RestQueryConfig restQueryConfig, List<Map<String, Object>> records, int startLine,
                      Font header, Font content) throws DocumentException {
        Paragraph paragraph = new Paragraph();
        addEmpty(paragraph, startLine-headerLine);
        document.add(paragraph);

        if (records.size() == 0) return;

        int       size  = restQueryConfig.getFields().size() > 0 ? getTableSize(restQueryConfig) : records.get(0).size();
        PdfPTable table = new PdfPTable(size);
        table.setWidthPercentage(100);
        document.setPageSize(size > 7 ? PageSize.A4.rotate() : PageSize.A4);

        if (restQueryConfig.getFields().size() > 0) {
            for (RestQueryConfig.FieldConfig f : restQueryConfig.getFields()) {
                if (!f.getIsVisible()) continue;

                table.addCell(getCell(f.getName(), Element.ALIGN_CENTER, header));
            }
        }
        else {
            for (String key : records.get(0).keySet())
                table.addCell(getCell(key, Element.ALIGN_CENTER, header));
        }
        table.setHeaderRows(1);

        for (Map<String, Object> record : records) {
            if (restQueryConfig.getFields().size() > 0) {
                for (RestQueryConfig.FieldConfig f : restQueryConfig.getFields()) {
                    if (!f.getIsVisible()) continue;

                    Object value = record.get(f.getId());
                    int    align = f.getType() == BaseMessageColumn.ColumnType.INTEGER ||
                                   f.getType() == BaseMessageColumn.ColumnType.NUMERIC ||
                                   f.getType() == BaseMessageColumn.ColumnType.PERCENTAGE ?
                                   Element.ALIGN_RIGHT : Element.ALIGN_LEFT;
                    table.addCell(getCell(value, align, content));
                }
            }
            else {
                for (String key : records.get(0).keySet())
                    table.addCell(getCell(record.get(key), Element.ALIGN_LEFT, content));
            }
        }
        document.add(table);
    }

    private PdfPCell getCell(Object value, int alignment, Font font) {
        PdfPCell column = new PdfPCell(new Phrase(String.valueOf(value), font));
        column.setHorizontalAlignment(alignment);
        return column;
    }

    private int getTableSize(RestQueryConfig config) {
        int size = 0;
        for (RestQueryConfig.FieldConfig f : config.getFields())
            if (f.getIsVisible()) size++;
        return size;
    }

    @Override
    public void close() throws IOException {
        document.close();
        outputStream.close();
    }
}
