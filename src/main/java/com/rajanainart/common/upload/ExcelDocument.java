package com.rajanainart.common.upload;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.rajanainart.common.rest.RestQueryConfig;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

public class ExcelDocument implements Closeable {

    private InputStream  stream   ;
    private String       fileName ;
    private Workbook     workbook ;
    private String       sheetName;
    private String       message = "";
    private OutputStream outputStream;
    private List<Map<String, Object>> records;
    private RestQueryConfig config;

    public boolean isXlsx() {
        return fileName.toLowerCase(Locale.ENGLISH).endsWith(".xlsx");
    }

    public Sheet getSheet() {
        return sheetName != null && !sheetName.isEmpty() ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
    }

    public Workbook getWorkbook() { return workbook; }

    public ExcelDocument(MultipartFile uploadFile) throws IOException {
        this (uploadFile.getInputStream(), uploadFile.getOriginalFilename());
    }

    public ExcelDocument(MultipartFile uploadFile, String sheetName) throws IOException {
        this (uploadFile.getInputStream(), uploadFile.getOriginalFilename());
        this.sheetName = sheetName;
    }

    public ExcelDocument(InputStream stream, String fileName) throws IOException {
        this.stream   = stream;
        this.fileName = fileName;
        init();
    }

    public ExcelDocument(OutputStream outputStream, String message, String fileName) throws IOException {
        this.fileName     = fileName;
        this.records      = new ArrayList<>();
        this.message      = message ;
        this.outputStream = outputStream;
        init();
        writeRecords();
    }

    public ExcelDocument(OutputStream outputStream, RestQueryConfig config, List<Map<String, Object>> records, String fileName) throws IOException {
        this.fileName     = fileName;
        this.records      = records ;
        this.outputStream = outputStream;
        this.config       = config;
        init();
        writeRecords();
    }

    private void init() throws IOException {
        if (isXlsx() && stream != null)
            workbook = new XSSFWorkbook(stream);
        else if (!isXlsx() && stream != null)
            workbook = new HSSFWorkbook(stream);
        else if (isXlsx() && stream == null)
            workbook = new XSSFWorkbook();
        else if (!isXlsx() && stream == null)
            workbook = new HSSFWorkbook();
    }

    private void writeRecords() throws IOException {
        int rowNumber  = 0;
        int cellNumber = 0;

        Sheet outputSheet = workbook.createSheet();
        Row   row         = outputSheet.createRow(rowNumber);

        if (!message.isEmpty())
            row.createCell(cellNumber).setCellValue(message);
        else if (records.size() == 0)
            row.createCell(cellNumber).setCellValue("No records found");
        else {
            if (config.getFields().size() > 0) {
                for (RestQueryConfig.FieldConfig f : config.getFields()) {
                    if (!f.getIsVisible()) continue;
                    row.createCell(cellNumber++).setCellValue(f.getName());
                }
            }
            else {
                for (String key : records.get(0).keySet())
                    row.createCell(cellNumber++).setCellValue(key);
            }

            for (Map<String, Object> record : records) {
                rowNumber++;
                cellNumber = 0;

                Row row1 = outputSheet.createRow(rowNumber);

                if (config.getFields().size() > 0) {
                    for (RestQueryConfig.FieldConfig f : config.getFields()) {
                        if (!f.getIsVisible()) continue;
                        String id = f.getId().toLowerCase(Locale.ENGLISH);
                        if(String.valueOf(record.get(id)).isEmpty()) {
                            row1.createCell(cellNumber++).setCellValue("");
                            continue;
                        }
                        switch (f.getType()) {
                            case PERCENTAGE:
                                row1.createCell(cellNumber++).setCellValue(Double.parseDouble(String.valueOf(record.get(id))));
                                CellStyle per =  workbook.createCellStyle();
                                per.setDataFormat(workbook.createDataFormat().getFormat("0%"));
                                row1.setRowStyle(per);
                                break;
                            case INTEGER:
                            case NUMERIC:
                                row1.createCell(cellNumber++).setCellValue(Double.parseDouble(String.valueOf(record.get(id))));
                                CellStyle style =  workbook.createCellStyle();
                                style.setDataFormat(workbook.createDataFormat().getFormat("0"));
                                row1.setRowStyle(style);
                                break;
                            case TEXT:
                                row1.createCell(cellNumber++).setCellValue(String.valueOf(record.get(id)));
                                break;
                        }
                    }
                }
                else {
                    for (Map.Entry<String, Object> entry : record.entrySet())
                        row1.createCell(cellNumber++).setCellValue(String.valueOf(entry.getValue()));
                }
            }
        }
        workbook.write(outputStream);
        outputStream.close();
    }

    public List<Map<String, Object>> getAllRecords(UploadConfig config) {
        List<Map<String, Object>> records1 = new ArrayList<>();
        for (Row row : getSheet()) {
            Map<String, Object> columns = new HashMap<>();
            for (UploadConfig.ColumnConfig column : config.getUploadColumns()) {
                if (!column.getIsVisible()) continue;

                Cell cell = row.getCell(column.getIndex());
                if (cell != null) {
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            if (String.valueOf(cell.getNumericCellValue()).endsWith(".0"))
                                columns.put(column.getId(), String.valueOf(cell.getNumericCellValue()).replaceAll(".0", ""));
                            else
                                columns.put(column.getId(), String.valueOf(cell.getNumericCellValue()));
                            break;
                        case BOOLEAN:
                            columns.put(column.getId(), String.valueOf(cell.getBooleanCellValue()));
                            break;
                        default:
                            columns.put(column.getId(), cell.getStringCellValue());
                            break;
                    }
                }
                else
                    columns.put(column.getId(), "");
            }
            if (columns.size() != 0)
                records1.add(columns);
        }
        return records1;
    }

    public void createUploadErrorExcel(OutputStream outputStream, UploadConfig config, List<Upload.ValidationError> errors) throws IOException {
        try (Workbook outputWorkbook = new XSSFWorkbook()) {
            Sheet     outputSheet    = outputWorkbook.createSheet();

            int rowNumber  = 0;
            int cellNumber = 0;
            Row row = outputSheet.createRow(rowNumber);
            for (UploadConfig.ColumnConfig column : config.getUploadColumns()) {
                if (!column.getIsVisible()) continue;
                row.createCell(cellNumber++).setCellValue(column.getId());
            }
            row.createCell(cellNumber++).setCellValue("Line No");
            row.createCell(cellNumber++).setCellValue("Error");

            for (Upload.ValidationError error : errors) {
                cellNumber = 0;
                rowNumber++;

                row = outputSheet.createRow(rowNumber);
                for (UploadConfig.ColumnConfig column : config.getUploadColumns()) {
                    if (!column.getIsVisible()) continue;
                    row.createCell(cellNumber++).setCellValue(String.valueOf(error.UploadedDetails.getOrDefault(column.getId(), "")));
                }
                row.createCell(cellNumber++).setCellValue(error.LineNo);
                row.createCell(cellNumber++).setCellValue(Arrays.toString(error.ErrorDetails.toArray()));
            }
            outputWorkbook.write(outputStream);
            outputStream.close();
        }
    }

    @Override
    public void close() throws IOException {
        workbook.close();
    }
}
