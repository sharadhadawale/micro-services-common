package com.rajanainart.upload;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.RestQueryConfig;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

public class ExcelDocument implements Closeable {

    private InputStream  stream   ;
    private String       fileName ;
    private Workbook     workbook ;
    private String       sheetName = "";
    private OutputStream outputStream;
    private Sheet        outputSheet;

    public boolean isXlsx() {
        return fileName.toLowerCase(Locale.ENGLISH).endsWith(".xlsx");
    }

    public Workbook getWorkbook() { return workbook; }

    public void setSheetName(String name) { sheetName = name; }

    public Sheet getSheet() {
        if (stream != null)
            return sheetName != null && !sheetName.isEmpty() ? workbook.getSheet(sheetName) : workbook.getSheetAt(0);
        else {
            if (outputSheet == null) {
                String name = sheetName.isEmpty() ? "Sheet1" : sheetName;
                outputSheet = workbook.createSheet(getValidExcelName(name));
            }
            return outputSheet;
        }
    }

    public ExcelDocument(MultipartFile uploadFile) throws IOException {
        this(uploadFile, "");
    }

    public ExcelDocument(MultipartFile uploadFile, String sheetName) throws IOException {
        this.stream    = uploadFile.getInputStream();
        this.fileName  = uploadFile.getOriginalFilename();
        this.sheetName = sheetName;
        init();
    }

    public ExcelDocument(OutputStream outputStream, String fileName) throws IOException {
        this.fileName     = fileName;
        this.outputStream = outputStream;
        init();
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

    public void writeHeader(UploadConfig uploadConfig, List<String> additionalColumns) throws IOException {
        int cellNumber = 0;
        Row row        = getSheet().createRow(0);

        uploadConfig.getUploadColumns().sort(Comparator.comparingInt(UploadConfig.ColumnConfig::getIndex));
        for (UploadConfig.ColumnConfig column : uploadConfig.getUploadColumns()) {
            if (!column.getIsVisible()) continue;

            row.createCell(cellNumber++).setCellValue(column.getName());
        }
        for (String addl : additionalColumns)
            row.createCell(cellNumber++).setCellValue(addl);
    }

    public void writeString(String message, int startRow, int startCol) throws IOException {
        int rowNumber  = startRow;
        int cellNumber = startCol;
        Row row        = getSheet().createRow(rowNumber);
        row.createCell(cellNumber).setCellValue(message);
    }

    public void writeRecords(RestQueryConfig config, List<Map<String, Object>> records, int startRow, int startCol) throws IOException {
        int rowNumber  = startRow;
        int cellNumber = startCol;
        Row row        = getSheet().createRow(rowNumber);

        if (records.size() == 0) {
            row.createCell(cellNumber).setCellValue("No records found");
        }
        else {
            writeSelectList(config, records.get(0));

            if (config.getFields().size() > 0) {
                for (RestQueryConfig.FieldConfig f : config.getFields()) {
                    if (!f.getIsVisible()) continue;
                    autoSetColumnSize(getSheet(), cellNumber);
                    row.createCell(cellNumber++).setCellValue(f.getName());
                }
            }
            else {
                for (String key : records.get(0).keySet()) {
                	autoSetColumnSize(getSheet(), cellNumber);
                    row.createCell(cellNumber++).setCellValue(key);
                }
            }

            boolean isFirstRow = true;
            for (Map<String, Object> record : records) {
                rowNumber++;
                cellNumber = 0;

                Row row1 = getSheet().createRow(rowNumber);

                if (config.getFields().size() > 0) {
                    for (RestQueryConfig.FieldConfig f : config.getFields()) {
                        if (!f.getIsVisible()) continue;
                        String id = f.getId(); //.toLowerCase(Locale.ENGLISH);
                        if(String.valueOf(record.get(id)).isEmpty()) {
                            row1.createCell(cellNumber++).setCellValue("");
                            continue;
                        }
                        switch (f.getType()) {
                            case PERCENTAGE:
                            	autoSetColumnSize(getSheet(), cellNumber);
                            	if (record.containsKey(id) && record.get(id) != null)
                                    row1.createCell(cellNumber++).setCellValue(Double.parseDouble(String.valueOf(record.get(id))));
                            	else
                                    row1.createCell(cellNumber++).setCellValue("");
                                CellStyle per =  workbook.createCellStyle();
                                per.setDataFormat(workbook.createDataFormat().getFormat("0%"));
                                row1.setRowStyle(per);
                                break;
                            case INTEGER:
                                getSheet().autoSizeColumn(cellNumber);
                                if (record.containsKey(id) && record.get(id) != null)
                                    row1.createCell(cellNumber++).setCellValue(Long.parseLong(String.valueOf(record.get(id))));
                                else
                                    row1.createCell(cellNumber++).setCellValue("");
                                CellStyle style1 =  workbook.createCellStyle();
                                style1.setDataFormat(workbook.createDataFormat().getFormat("0"));
                                row1.setRowStyle(style1);
                                break;
                            case NUMERIC:
                                getSheet().autoSizeColumn(cellNumber);
                                if (record.containsKey(id) && record.get(id) != null)
                                    row1.createCell(cellNumber++).setCellValue(Double.parseDouble(String.valueOf(record.get(id))));
                                else
                                    row1.createCell(cellNumber++).setCellValue("");
                                CellStyle style =  workbook.createCellStyle();
                                style.setDataFormat(workbook.createDataFormat().getFormat("0"));
                                row1.setRowStyle(style);
                                break;
                            case TEXT:
                            	autoSetColumnSize(getSheet(), cellNumber);
                                if (record.containsKey(id) && record.get(id) != null)
                                    row1.createCell(cellNumber++).setCellValue(String.valueOf(record.get(id)));
                                else
                                    row1.createCell(cellNumber++).setCellValue("");
                                break;
                            case SELECT:
                            case SINGLE_SELECT:
                                if (isFirstRow)
                                    defineSelects(getSheet(), id, startRow+1, cellNumber, records.size()+startRow, cellNumber);
                                autoSetColumnSize(getSheet(), cellNumber);
                                if (record.containsKey(id) && record.get(id) != null)
                                    row1.createCell(cellNumber++).setCellValue(String.valueOf(record.get(id)));
                                else
                                    row1.createCell(cellNumber++).setCellValue("");
                                break;
                        }
                    }
                    if (isFirstRow)
                        enableDataFilter(outputSheet, config, startRow, startCol);
                    isFirstRow = false;
                }
                else {
                    for (Map.Entry<String, Object> entry : record.entrySet()) {
                    	autoSetColumnSize(getSheet(), cellNumber);
                        row1.createCell(cellNumber++).setCellValue(String.valueOf(entry.getValue()));
                    }
                }
            }
        }
    }

    public void writeErrors(OutputStream responseStream, UploadConfig config, List<Upload.ValidationError> errors) throws IOException {
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
            outputWorkbook.write(responseStream);
            responseStream.close();
        }
    }

    public List<Map<String, Object>> getAllRecords(UploadConfig config) {
        List<Map<String, Object>> records1 = new ArrayList<>();
        for (Row row : getSheet()) {
            Map<String, Object> columns = new LinkedHashMap<>();
            for (UploadConfig.ColumnConfig column : config.getUploadColumns()) {
                if (!column.getIsVisible() && !column.isDynamicField()) continue;

                Cell cell = row.getCell(column.getIndex());
                if (cell != null) {
                    switch (cell.getCellType()) {
                        case NUMERIC:
                            if (String.valueOf(cell.getNumericCellValue()).endsWith(".0"))
                                columns.put(column.getId(), String.valueOf(cell.getNumericCellValue()).replaceAll("\\.0", ""));
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

    @Override
    public void close() throws IOException {
        if (outputSheet != null)
            workbook.write(outputStream);
        workbook.close();
        if (stream != null) stream.close();
        if (outputStream != null) outputStream.close();
    }

    private void autoSetColumnSize(Sheet outputSheet,int cellNumber) {
    	outputSheet.autoSizeColumn(cellNumber);
    }

    private Map<String, SelectInfo> selects = new HashMap<>();

    private Sheet writeSelectList(RestQueryConfig config, Map<String, Object> record) {
        int selectCount = 0;
        List<RestQueryConfig.FieldConfig> mSelects = config.getFieldsOfType(BaseMessageColumn.ColumnType.SELECT, true);
        List<RestQueryConfig.FieldConfig> sSelects = config.getFieldsOfType(BaseMessageColumn.ColumnType.SINGLE_SELECT, true);

        selectCount += mSelects.size();
        selectCount += sSelects.size();

        if (selectCount == 0) return null;

        Sheet selectSheet = workbook.createSheet("Selects");
        writeSelectList(record, mSelects, selectSheet, 0);
        writeSelectList(record, sSelects, selectSheet, mSelects.size());
        workbook.setSheetVisibility(workbook.getSheetIndex(selectSheet.getSheetName()), SheetVisibility.HIDDEN);
        return selectSheet;
    }

    private void writeSelectList(Map<String, Object> record, List<RestQueryConfig.FieldConfig> mSelects, Sheet selectSheet, int startRow) {
        int rowIndex = startRow;
        int colIndex = 0;
        for (RestQueryConfig.FieldConfig f : mSelects) {
            List<Map<String, Object>> list = (List<Map<String, Object>>)record.get(String.format("%s_array", f.getId()));
            if (list == null || list.size() == 0) continue;

            colIndex = 0;
            Row row  = selectSheet.createRow(rowIndex);
            for (Map<String, Object> value : list) {
                String fieldName = getDescriptionFieldName(f, list.get(0));
                row.createCell(colIndex++).setCellValue(String.valueOf(value.get(fieldName)));
            }

            SelectInfo info = new SelectInfo();
            info.sheetName  = selectSheet.getSheetName();
            info.rowIndex   = rowIndex;
            info.colIndex   = colIndex-1;
            selects.put(f.getId(), info);

            defineValidationName(selectSheet, f.getId());
            rowIndex++;
        }
    }

    private String getDescriptionFieldName(RestQueryConfig.FieldConfig field, Map<String, Object> record) {
        String fieldName = "description";
        RestQueryConfig config = BaseRestController.REST_QUERY_CONFIGS.get(field.getSelectQuery());
        if (config != null && config.getFields().size() > 0) {
            for (RestQueryConfig.FieldConfig f : config.getFields()) {
                if (f.getName().equalsIgnoreCase(fieldName))
                    return f.getId();
            }
            List<RestQueryConfig.FieldConfig> fields = config.getFieldsOfType(BaseMessageColumn.ColumnType.TEXT, true);
            if (fields.size() > 0)
                return fields.get(0).getId();
        }
        for (String key : record.keySet()) {
            if (key.contains("ERR") || key.endsWith("ID")) continue;

            return key;
        }
        return fieldName;
    }

    private void defineValidationName(Sheet selectSheet, String fieldId) {
        SelectInfo selectInfo = selects.get(fieldId);
        Name name = selectSheet.getWorkbook().createName();
        name.setRefersToFormula(selectInfo.getExcelStringReference());
        name.setNameName(fieldId);
    }

    private void defineSelects(Sheet sourceSheet, String fieldId, int startRow, int startCol, int endRow, int endCol) {
        CellRangeAddressList addressList      = new CellRangeAddressList(startRow, endRow, startCol, endCol);
        DataValidationHelper validationHelper = sourceSheet.getDataValidationHelper();
        DataValidationConstraint constraint   = validationHelper.createFormulaListConstraint(fieldId);
        DataValidation validation = validationHelper.createValidation(constraint, addressList);
        validation.setShowErrorBox(true);
        validation.createErrorBox(fieldId, String.format("Please select a valid value from the list: %s", fieldId));
        sourceSheet.addValidationData(validation);
    }

    private void enableDataFilter(Sheet sheet, RestQueryConfig config, int startRow, int startCol) {
        int              count = config.getVisibleFieldsCount();
        CellRangeAddress range = new CellRangeAddress(startRow, startRow, startCol, count-1);
        sheet.setAutoFilter(range);
    }

    private class SelectInfo {
        String sheetName;
        int rowIndex;
        int colIndex;

        String getExcelStringReference() {
            CellReference startCell = new CellReference(sheetName, rowIndex, 0, true, true);
            CellReference endCell   = new CellReference(sheetName, rowIndex, colIndex, true, true);
            return String.format("%s:%s", startCell.formatAsString(), endCell.formatAsString());
        }
    }

    public static String getValidExcelName(String name) {
        return name.replaceAll("[\\\\\\/\\?\\[\\]\\:\\?]+", " ");
    }
}
