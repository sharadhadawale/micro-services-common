package com.rajanainart.common.upload;

import java.util.List;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

public class ExcelUtils {
	private ExcelUtils() {}

	public static Sheet generateSheetandHeaders(Sheet sheet, List<String> cellHeaders, int rowNumber) {
		Row header = sheet.createRow(rowNumber);
		for (int i = 0; i < cellHeaders.size(); i++) {
			header.createCell(i).setCellValue(cellHeaders.get(i));
		}
		return sheet;
	}
}

