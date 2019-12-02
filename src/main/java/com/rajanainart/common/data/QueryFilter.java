package com.rajanainart.common.data;

public class QueryFilter {
    public enum FilterOperator {
        LIKE 	  ("LIKE"),
        NOT_LIKE  ("NOT LIKE"),
        EQUAL 	  ("="),
        NOT_EQUAL ("!="),
        IN 		  ("IN"),
        NOT_IN 	  ("NOT IN"),
        NULL 	  ("NULL"),
        NOT_NULL  ("NOT NULL"),
        GREATER_THAN (">"),
        GREATER_THAN_EQUAL_TO (">="),
        LESSER_THAN  ("<"),
        LESSER_THAN_EQUAL_TO ("<="),
        BETWEEN 	 ("BETWEEN"),
        NOT_BETWEEN  ("NOT BETWEEN");

        private final String text;

        private FilterOperator(String text) {
            this.text = text;
        }

        public static FilterOperator valueFromAlias(String alias) {
            for (FilterOperator op : FilterOperator.values()) {
                if (op.toString().equals(alias))
                    return op;
            }
            return LIKE;
        }

        @Override
        public String toString() { return text; }
    }


    private String fieldId   = "";
    private String fieldName = "";
    private String fieldType = "TEXT";
    private String operator  = "";
    private String value1    = "";
    private String value2    = "";

    public String getFieldId  () { return fieldId  ; }
    public String getFieldName() { return fieldName; }
    public String getFieldType() { return fieldType; }
    public String getOperator () { return operator ; }
    public String getValue1   () { return value1   ; }
    public String getValue2   () { return value2   ; }

    public BaseMessageColumn.ColumnType getActualFieldType() {
        return BaseMessageColumn.ColumnType.valueOf(fieldType);
    }

    public FilterOperator getActualOperator() {
        return FilterOperator.valueFromAlias(operator);
    }

    public void setFieldId	(String id  ) { fieldId   = id  ; }
    public void setFieldName(String name) { fieldName = name; }
    public void setFieldType(String type) { fieldType = type; }
    public void setOperator (String op  ) { operator  = op  ; }
    public void setValue1   (String v1  ) { value1    = v1  ; }
    public void setValue2   (String v2  ) { value2    = v2  ; }
}
