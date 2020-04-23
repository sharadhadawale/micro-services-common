package com.rajanainart.rest;

import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import com.rajanainart.config.BaseConfig;
import com.rajanainart.data.Database;
import com.rajanainart.helper.ReflectionHelper;
import com.rajanainart.helper.XmlNodeHelper;
import com.rajanainart.integration.iaas.IaaSRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.rajanainart.config.XmlConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.data.BaseMessageTable;

@Component("rest-query-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RestQueryConfig implements XmlConfig, BaseMessageTable {

    public static final int DEFAULT_PAGE_SIZE = 25;

    public enum RestQueryType    { SELECT, DML, PROC  }
    public enum RestQueryUsedFor { RESULT, META }

    public enum RestQueryContentType {
        XML, JSON, CSV, TEXT, XLS, XLSX;

        @Override
        public String toString() {
            return this.name().toLowerCase();
        }
    }

    private Optional<Integer> pageSize;
    private String id;
    private String name;
    private String query;
    private String actualQuery;
    private String service;
    private String action;
    private String entity;
    private String target;
    private String sourceDb = "";
    private String[] flags   = new String[] {};
    private boolean editable = false;
    private boolean cache    = false;
    private int     expiryMinutes = 120;
    private RestQueryType qType = RestQueryType.SELECT;
    private RestQueryUsedFor usedFor = RestQueryUsedFor.RESULT;
    private RestQueryContentType responseType = RestQueryContentType.JSON;
    private ArrayList<FieldConfig> fields = new ArrayList<>();
    private ArrayList<ParameterConfig> params = null;
    private ArrayList<MandatoryValidator> mandatory = null;
    private ArrayList<TypeValidator> type = null;
    private ArrayList<LogicalValidator> logical = null;

    @JsonIgnore public String getQuery      () { return query      ; }
    @JsonIgnore public String getActualQuery() { return actualQuery; }

    public String getId    () { return id    ; }
    public String getName  () { return name  ; }
    public String getTarget() { return target; }
    public String  getSourceDb   () { return sourceDb; }
    public String  getServiceName() { return service ; }
    public String  getActionName () { return action  ; }
    public String  getEntityName () { return entity  ; }
    public boolean getIsEditable () { return editable; }
    public String[] getFlags     () { return flags   ; }
    public boolean isCacheEnabled() { return cache   ; }
    public int   getExpiryMinutes() { return expiryMinutes; }

    public void updateQuery(String query) { this.query = query; }

    public ArrayList<ParameterConfig> getParameters() { return params; }
    @JsonIgnore public ArrayList<MandatoryValidator> getMandatoryValidators() { return mandatory; }
    @JsonIgnore public ArrayList<TypeValidator     > getTypeValidators     () { return type	    ; }
    @JsonIgnore public ArrayList<LogicalValidator  > getLogicalValidators  () { return logical	; }

    public RestQueryContentType getResponseContentType() { return responseType; }
    public RestQueryType        getRestQueryType      () { return qType       ; }
    public RestQueryUsedFor     getRestQueryUsedFor   () { return usedFor     ; }
    public List<FieldConfig>    getFields             () { return fields      ; }

    public FieldConfig getPKField() {
        for (FieldConfig f : fields)
            if (f.getIsPk()) return f;
        return null;
    }

    public List<FieldConfig> getPKFields() {
        List<FieldConfig> list = new ArrayList<>();
        for (FieldConfig f : fields)
            if (f.getIsPk()) list.add(f);
        return list;
    }

    public List<FieldConfig> getFieldsOfType(BaseMessageColumn.ColumnType type, boolean visibility) {
        List<FieldConfig> list = new ArrayList<>();
        for (FieldConfig f : fields) {
            if (f.getType() == type && f.getIsVisible() == visibility)
                list.add(f);
        }
        return list;
    }

    public int getVisibleFieldsCount() {
        final AtomicReference<Integer> count = new AtomicReference<>(0);
        fields.forEach(x -> {
            if (x.getIsVisible())
                count.set(count.get()+1);
        });
        return count.get();
    }

    public int getPageSize() {
        return pageSize.isPresent() ? pageSize.get() : DEFAULT_PAGE_SIZE;
    }

    @JsonIgnore
    public ArrayList<BaseMessageColumn> getColumns() {
        ArrayList<BaseMessageColumn> list = new ArrayList<>();
        for (FieldConfig c : fields)
            list.add(c);
        return list;
    }

    public static RestQueryConfig getInstance(IaaSRequest iaaSRequest) {
        RestQueryConfig config = new RestQueryConfig();
        String key    = iaaSRequest.getRESTQueryName();
        config.id     = config.name = key;
        config.query  = iaaSRequest.getQuery();
        config.target = iaaSRequest.getTargetTable();
        config.fields = new ArrayList<>();
        config.params = new ArrayList<>();
        config.type   = new ArrayList<>();
        config.logical   = new ArrayList<>();
        config.mandatory = new ArrayList<>();

        return config;
    }

    public void buildFields(Database db, IaaSRequest iaaSRequest) {
        Map<String, String> columns = db.getResultSetColumns(query);
        for (Map.Entry<String, String> entry : columns.entrySet()) {
            FieldConfig field = this.new FieldConfig(entry.getKey(), entry.getValue());
            if (iaaSRequest.getPkCol().equalsIgnoreCase(field.getId()))
                field.isPk = true;
            fields.add(field);
        }
    }

    public synchronized void configure(Node node) {
        synchronized (this) {
            id      = XmlNodeHelper.getAttributeValue(node, "id"   );
            name    = XmlNodeHelper.getAttributeValue(node, "name" );
            query   = XmlNodeHelper.getAttributeValue(node, "query");
            service = XmlNodeHelper.getAttributeValue(node, "service");
            action  = XmlNodeHelper.getAttributeValue(node, "action" );
            entity  = XmlNodeHelper.getAttributeValue(node, "entity" );
            target  = XmlNodeHelper.getAttributeValue(node, "target" );
            sourceDb= XmlNodeHelper.getAttributeValue(node, "source-db");
            actualQuery = query;

            if (id.isEmpty() || name.isEmpty())
                throw new NullPointerException("Attributes id & name are mandatory");

            pageSize = Optional.empty();
            if (!XmlNodeHelper.getAttributeValue(node, "page-size").isEmpty())
                pageSize = Optional.of(XmlNodeHelper.getAttributeValueAsInteger(node, "page-size"));

            String value   = XmlNodeHelper.getAttributeValue(node, "is-editable");
            if (!value.isEmpty())
                editable = XmlNodeHelper.getAttributeValueAsBoolean(node, "is-editable");

            value = XmlNodeHelper.getAttributeValue(node, "cache");
            if (!value.isEmpty())
                cache = XmlNodeHelper.getAttributeValueAsBoolean(node, "cache");

            if (!XmlNodeHelper.getAttributeValue(node, "cache-expiry-minutes").isEmpty())
                expiryMinutes = XmlNodeHelper.getAttributeValueAsInteger(node, "cache-expiry-minutes");

            Node n = XmlNodeHelper.getChildNode(node, "response-content-type");
            if (n != null) {
                value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    responseType = XmlNodeHelper.getNodeValueAsEnum(RestQueryContentType.class, n);
            }

            value = XmlNodeHelper.getAttributeValue(node, "query-type");
            if (!value.isEmpty())
                qType = XmlNodeHelper.getNodeAttributeValueAsEnum(RestQueryType.class, node, "query-type");

            value = XmlNodeHelper.getAttributeValue(node, "query-used-for");
            if (!value.isEmpty())
                usedFor = XmlNodeHelper.getNodeAttributeValueAsEnum(RestQueryUsedFor.class, node, "query-used-for");

            value = XmlNodeHelper.getAttributeValue(node, "flags");
            if (!value.isEmpty())
                flags = value.split(",");

            ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "field");
            fields = new ArrayList<>();
            for (int idx=0; idx<nodes.size(); idx++)
                fields.add(new FieldConfig(nodes.get(idx)));

            ArrayList<Node> p = XmlNodeHelper.getChildNodes(node, "parameter");
            params = new ArrayList<>();
            for (int idx=0; idx<p.size(); idx++)
                params.add(new ParameterConfig(p.get(idx)));

            ArrayList<Node> v = XmlNodeHelper.getChildNodes(node, "mandatory-validator");
            mandatory = new ArrayList<>();
            for (int idx=0; idx<v.size(); idx++)
                mandatory.add(new MandatoryValidator(v.get(idx)));

            v = XmlNodeHelper.getChildNodes(node, "type-validator");
            type = new ArrayList<>();
            for (int idx=0; idx<v.size(); idx++)
                type.add(new TypeValidator(v.get(idx)));

            v = XmlNodeHelper.getChildNodes(node, "logical-validator");
            logical = new ArrayList<>();
            for (int idx=0; idx<v.size(); idx++)
                logical.add(new LogicalValidator(v.get(idx)));
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", id, name, qType.toString());
    }

    public boolean hasCursorParameter() {
        if (params == null) return false;

        for (ParameterConfig p : params) {
            if (p.getType() == ParameterType.CURSOR || p.getType() == ParameterType.NUMERIC_ARRAY ||
                p.getType() == ParameterType.STRING_ARRAY || p.getType() == ParameterType.RECORD_OBJECT ||
                p.isOutput()) return true;
        }
        return false;
    }

    public enum ParameterType { SCALAR, CURSOR, STRING_ARRAY, NUMERIC_ARRAY, RECORD_OBJECT }

    public class ParameterConfig extends BaseConfig {
        private boolean       output = false;
        private String  defaultValue = "";
        private String        field  = "";
        private ParameterType type   = ParameterType.SCALAR;

        public boolean       isOutput() { return output; }
        public ParameterType getType () { return type  ; }
        public String getDefaultValue() { return defaultValue; }
        public String getFieldId     () { return field ; }

        public List<String> getDefaultValueAsStringArray() {
            String[] result = defaultValue.trim().isEmpty() ? new String[] {} : defaultValue.split(",");
            return Arrays.asList(result);
        }

        public <T extends Number> List<T> getDefaultValueAsNumberArray(Class<T> tClass) {
            List<String> defaultValues = getDefaultValueAsStringArray();
            T[]          result        = (T[])Array.newInstance(tClass, defaultValues.size());

            int idx = 0;
            for (String value : defaultValues)
                result[idx++] = ReflectionHelper.convertStringToNumber(tClass, value);
            return Arrays.asList(result);
        }

        public ParameterConfig(Node node) {
            super(node);

            String value = XmlNodeHelper.getAttributeValue(node, "output");
            if (!value.isEmpty())
                output = XmlNodeHelper.getAttributeValueAsBoolean(node, "output");

            value = XmlNodeHelper.getAttributeValue(node, "type");
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(ParameterType.class, node, "type");

            defaultValue = XmlNodeHelper.getAttributeValue(node, "default-value");
            field        = XmlNodeHelper.getAttributeValue(node, "field-id"     );
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s:%s", id, name, type.toString(), defaultValue);
        }
    }

    public class FieldConfig extends BaseConfig implements BaseMessageColumn {

        private BaseMessageColumn.ColumnType type = BaseMessageColumn.ColumnType.TEXT;
        private String format;
        private String query ;
        private String target;
        private String dependents;
        private String[] flags = new String[] {};
        private boolean mandatory = false;
        private boolean editable  = false;
        private boolean visible   = true;
        private boolean isPk      = false;
        private boolean autoIncr  = false;
        private boolean gridHide  = false;

        public BaseMessageColumn.ColumnType getType() { return type; }
        public String[] getFlags      () { return flags    ; }
        public String   getFormat     () { return format   ; }
        public String   getSelectQuery() { return query    ; }
        public String   getTargetField() { return target   ; }
        public String[] getDependents () { return dependents.trim().isEmpty() ? new String[] {} : dependents.split(","); }
        @JsonProperty("mandatory")
        public boolean  getIsMandatory() { return mandatory; }
        @JsonProperty("editable")
        public boolean  getIsEditable () { return editable ; }
        @JsonProperty("visible")
        public boolean  getIsVisible  () { return visible  ; }
        @JsonProperty("hide")
        public boolean  getGridHide   () { return gridHide ; }
        @JsonProperty("pk")
        public boolean  getIsPk       () { return isPk     ; }
        @JsonIgnore
        public boolean  isDynamicField() { return false    ; }
        public boolean  getAutoIncr   () { return autoIncr ; }
        public int      getIndex      () { return -1; }

        FieldConfig(String column, String dataType) {
            id = name = target = column;
            switch (dataType) {
                case "int":
                case "int2":
                case "int4":
                case "int8":
                    type = ColumnType.INTEGER;
                    break;
                case "decimal":
                case "float4":
                case "float8":
                    type = ColumnType.NUMERIC;
                    break;
                case "date":
                case "timetz":
                case "timestamptz":
                    type = ColumnType.DATE;
                    break;
                default:
                    type = ColumnType.TEXT;
                    break;
            }
        }

        public FieldConfig(Node node) {
            super(node);
            dependents   = XmlNodeHelper.getAttributeValue(node, "dependents");
            String value = XmlNodeHelper.getAttributeValue(node, "type");
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(BaseMessageColumn.ColumnType.class, node, "type");
            value = XmlNodeHelper.getAttributeValue(node, "is-mandatory");
            if (!value.isEmpty())
                mandatory = XmlNodeHelper.getAttributeValueAsBoolean(node, "is-mandatory");

            String editKey = "is-editable";
            value = XmlNodeHelper.getAttributeValue(node, editKey);
            if (!value.isEmpty())
                editable = XmlNodeHelper.getAttributeValueAsBoolean(node, editKey);

            value = XmlNodeHelper.getAttributeValue(node, "is-visible");
            if (!value.isEmpty())
                visible = XmlNodeHelper.getAttributeValueAsBoolean(node, "is-visible");

            value = XmlNodeHelper.getAttributeValue(node, "grid-hide");
            if (!value.isEmpty())
                gridHide = XmlNodeHelper.getAttributeValueAsBoolean(node, "grid-hide");

            value = XmlNodeHelper.getAttributeValue(node, "is-pk");
            if (!value.isEmpty())
                isPk = XmlNodeHelper.getAttributeValueAsBoolean(node, "is-pk");

            value = XmlNodeHelper.getAttributeValue(node, "auto-increment");
            if (!value.isEmpty())
                autoIncr = XmlNodeHelper.getAttributeValueAsBoolean(node, "auto-increment");

            format = XmlNodeHelper.getAttributeValue(node, "format");
            query  = XmlNodeHelper.getAttributeValue(node, "query" );
            target = XmlNodeHelper.getAttributeValue(node, "target");
            value  = XmlNodeHelper.getAttributeValue(node, "flags");
            if (!value.isEmpty())
                flags = value.split(",");
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s", id, name, type.toString());
        }
    }

    public enum ValidationOperatorType  { AND, OR }
    public enum ValidationDataType		{ INTEGER, NUMERIC, DATE, TEXT }
    public enum ValidationExecutionType { PRE_DATA_FETCH, WHILE_DATA_FETCH, POST_DATA_FETCH }
    public enum ValidationLogicalType 	{ LESSER_THAN, LESSER_THAN_EQUAL_TO, GREATER_THAN, GREATER_THAN_EQUAL_TO,
        EQUAL_TO, NOT_EQUAL_TO, BETWEEN, NOT_BETWEEN }

    public abstract class BaseValidator {
        private List<String> params = null;
        private ValidationExecutionType execType = ValidationExecutionType.PRE_DATA_FETCH;

        public  List<String> 			getParamNames   () { return params  ; }
        public  ValidationExecutionType getExecutionType() { return execType; }
        protected BaseValidator(Node node) {
            params = new ArrayList<String>();

            int index     = 1;
            String value  = XmlNodeHelper.getAttributeValue(node, String.format("param%s", index));
            while (!value.isEmpty()) {
                params.add(value);
                value = XmlNodeHelper.getAttributeValue(node, String.format("param%s", ++index));
            }

            value = XmlNodeHelper.getAttributeValue(node, "execution-type");
            if (!value.isEmpty())
                execType = XmlNodeHelper.getNodeAttributeValueAsEnum(ValidationExecutionType.class, node, "execution-type");
        }
    }

    public class MandatoryValidator extends BaseValidator {

        private ValidationOperatorType opType = ValidationOperatorType.OR;

        public ValidationOperatorType getOperatorType() { return opType; }

        public MandatoryValidator(Node node) {
            super(node);

            String value = XmlNodeHelper.getAttributeValue(node, "operator-type");
            if (!value.isEmpty())
                opType = XmlNodeHelper.getNodeAttributeValueAsEnum(ValidationOperatorType.class, node, "operator-type");
        }
    }

    public class TypeValidator extends BaseValidator {

        private ValidationDataType type = ValidationDataType.TEXT;

        public ValidationDataType getDataType() { return type; }

        public TypeValidator(Node node) {
            super(node);

            String dtKey = "data-type";
            String value = XmlNodeHelper.getAttributeValue(node, dtKey);
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(ValidationDataType.class, node, dtKey);
        }
    }

    public class LogicalValidator extends BaseValidator {

        private ValidationLogicalType logicType = ValidationLogicalType.EQUAL_TO;
        private ValidationDataType    type 	 	= ValidationDataType.NUMERIC;

        public ValidationDataType 	 getDataType   () { return type		; }
        public ValidationLogicalType getLogicalType() { return logicType; }

        public LogicalValidator(Node node) {
            super(node);

            String dtKey = "data-type";
            String value = XmlNodeHelper.getAttributeValue(node, dtKey);
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(ValidationDataType.class, node, dtKey);

            value = XmlNodeHelper.getAttributeValue(node, "logic-type");
            if (!value.isEmpty())
                logicType = XmlNodeHelper.getNodeAttributeValueAsEnum(ValidationLogicalType.class, node, "logic-type");
        }
    }
}
