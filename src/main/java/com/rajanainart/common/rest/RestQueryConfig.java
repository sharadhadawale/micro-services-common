package com.rajanainart.common.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.rajanainart.common.config.BaseConfig;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.helper.XmlNodeHelper;
import com.rajanainart.common.integration.iaas.IaaSRequest;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.data.BaseMessageColumn;
import com.rajanainart.common.data.BaseMessageTable;

@Component("rest-query-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class RestQueryConfig implements XmlConfig, BaseMessageTable {

    public static final int DEFAULT_PAGE_SIZE = 25;

    public enum RestQueryType    { SELECT, DML  }
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
    private String  sourceDb = "";
    private boolean editable = false;
    private RestQueryType qType = RestQueryType.SELECT;
    private RestQueryUsedFor usedFor = RestQueryUsedFor.RESULT;
    private RestQueryContentType responseType = RestQueryContentType.JSON;
    private ArrayList<FieldConfig> fields = null;
    private ArrayList<ParameterConfig> params = null;
    private ArrayList<MandatoryValidator> mandatory = null;
    private ArrayList<TypeValidator> type = null;
    private ArrayList<LogicalValidator> logical = null;

    public String getId    () { return id    ; }
    public String getName  () { return name  ; }
    public String getQuery () { return query ; }
    public String getTarget() { return target; }
    public String  getActualQuery() { return actualQuery; }
    public String  getSourceDb   () { return sourceDb; }
    public String  getServiceName() { return service ; }
    public String  getActionName () { return action  ; }
    public String  getEntityName () { return entity  ; }
    public boolean getIsEditable () { return editable; }

    public void updateQuery(String query) { this.query = query; }

    public ArrayList<ParameterConfig   > getParameters	       () { return params	; }
    public ArrayList<MandatoryValidator> getMandatoryValidators() { return mandatory; }
    public ArrayList<TypeValidator     > getTypeValidators     () { return type	    ; }
    public ArrayList<LogicalValidator  > getLogicalValidators  () { return logical	; }

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

            String editKey = "is-editable";
            String value   = XmlNodeHelper.getAttributeValue(node, editKey);
            if (!value.isEmpty())
                editable = XmlNodeHelper.getAttributeValueAsBoolean(node, editKey);

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

    public class ParameterConfig extends BaseConfig {
        public ParameterConfig(Node node) {
            super(node);
        }
    }

    public class FieldConfig extends BaseConfig implements BaseMessageColumn {

        private BaseMessageColumn.ColumnType type = BaseMessageColumn.ColumnType.TEXT;
        private String format;
        private String query ;
        private String target;
        private boolean mandatory = false;
        private boolean editable  = false;
        private boolean visible   = true;
        private boolean isPk      = false;
        private boolean autoIncr  = false;

        public BaseMessageColumn.ColumnType getType() { return type; }
        public String   getFormat     () { return format   ; }
        public String   getSelectQuery() { return query    ; }
        public String   getTargetField() { return target   ; }
        @JsonProperty("mandatory")
        public boolean  getIsMandatory() { return mandatory; }
        @JsonProperty("editable")
        public boolean  getIsEditable () { return editable ; }
        @JsonProperty("visible")
        public boolean  getIsVisible  () { return visible  ; }
        @JsonProperty("pk")
        public boolean  getIsPk       () { return isPk     ; }
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

            value = XmlNodeHelper.getAttributeValue(node, "is-pk");
            if (!value.isEmpty())
                isPk = XmlNodeHelper.getAttributeValueAsBoolean(node, "is-pk");

            value = XmlNodeHelper.getAttributeValue(node, "auto-increment");
            if (!value.isEmpty())
                autoIncr = XmlNodeHelper.getAttributeValueAsBoolean(node, "auto-increment");

            format = XmlNodeHelper.getAttributeValue(node, "format");
            query  = XmlNodeHelper.getAttributeValue(node, "query" );
            target = XmlNodeHelper.getAttributeValue(node, "target");
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
