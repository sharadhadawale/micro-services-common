package com.rajanainart.common.data;

import java.util.Date;

import com.rajanainart.common.helper.MiscHelper;

public interface BaseMessageColumn {
    enum ColumnType { INTEGER, NUMERIC, DATE, TEXT, PERCENTAGE, SELECT, SUBLIST }

    String      getId    ();
    String      getName  ();
    ColumnType  getType  ();
    String 	    getFormat();
    String      getTargetField();
    String	    getSelectQuery();
    boolean     getIsMandatory();
    boolean	    getIsEditable ();
    boolean     getAutoIncr   ();
    boolean     getIsVisible  ();
    boolean     getIsPk       ();
    int         getIndex      ();

    default Object parseValue(Object inputValue) {
        if (inputValue == null) return "";

        String input = inputValue.toString();
        switch (getType()) {
            case NUMERIC:
                double value = MiscHelper.convertObjectToDouble(input);
                if (getFormat().isEmpty())
                    return value;
                else
                    return String.format(!getFormat().isEmpty() ? getFormat() : BaseEntity.DEFAULT_NUMERIC_OUTPUT_FORMAT, value);
            case INTEGER:
                return MiscHelper.convertObjectToLong(input);
            case DATE:
                Date date = MiscHelper.convertStringToDate(input, getFormat());
                return MiscHelper.convertDateToString(date, !getFormat().isEmpty() ? getFormat() : BaseEntity.DAFAULT_DATE_OUTPUT_FORMAT);
            default:
                return input.replaceAll("[\r\n\"]*", "");
        }
    }
}
