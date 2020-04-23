package com.rajanainart.xmlreport;

import java.util.*;

import javax.servlet.http.HttpServletRequest;

import com.rajanainart.config.BaseConfig;
import com.rajanainart.config.XmlConfig;
import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.data.QueryFilter;
import com.rajanainart.helper.UrlHelper;
import com.rajanainart.helper.XmlNodeHelper;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.RestQueryConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

@Component("xml-report-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XmlReportConfig implements XmlConfig {

    public enum PaginationAt { CLIENT, SERVER }
    public enum FilterType   { PARAMETER_BASED, RESULT_SET_BASED }

    private String id    = "";
    private String name  = "";
    private String query = "";

    private RestQueryConfig queryConfig;
    private RestQueryConfig insertQueryConfig;
    private RestQueryConfig updateQueryConfig;
    private RestQueryConfig deleteQueryConfig;
    private RestQueryConfig validateQueryConfig;
    private RestQueryConfig errorQueryConfig;
    private RestQueryConfig exportQueryConfig;
    private RestQueryConfig importQueryConfig;
    private RestQueryConfig roleQueryConfig;
    private RestQueryConfig authQueryConfig;

    private boolean hideFilter = false;
    private boolean hideExport = false;
    private boolean hidePager  = false;

    private HttpServletRequest request;
    private PaginationAt       pagination   = PaginationAt.CLIENT;
    private FilterType         filterType   = FilterType.PARAMETER_BASED;

    private ArrayList<String	  > export  = new ArrayList<>();
    private ArrayList<String	  > exclude = new ArrayList<>();
    private ArrayList<SelectConfig> selects = new ArrayList<>();
    private ArrayList<GroupConfig > groups  = new ArrayList<>();
    private ArrayList<LinkConfig  > links   = new ArrayList<>();
    private ArrayList<ExpandConfig> expands = new ArrayList<>();
    private ArrayList<JavaScriptValidationConfig> jsValidations = new ArrayList<>();
    private int[] sizes;

    public String getId   () { return id;    }
    public String getName () { return name;  }
    public int[]  getPageSizes() { return sizes; }
    public String getRestQuery() { return query; }

    public boolean getHideFilter() { return hideFilter; }
    public boolean getHideExport() { return hideExport; }
    public boolean getHidePager () { return hidePager ; }

    public PaginationAt    getPagination  () { return pagination       ; }
    public FilterType      getFilterType  () { return filterType       ; }
    public RestQueryConfig getSchema      () { return queryConfig      ; }
    public RestQueryConfig getInsertSchema() { return insertQueryConfig; }
    public RestQueryConfig getUpdateSchema() { return updateQueryConfig; }
    public RestQueryConfig getDeleteSchema() { return deleteQueryConfig; }
    public RestQueryConfig getErrorSchema () { return errorQueryConfig ; }
    public RestQueryConfig getImportSchema() { return importQueryConfig; }
    public RestQueryConfig getValidateSchema () { return validateQueryConfig; }
    public RestQueryConfig getRoleSchema() { return roleQueryConfig; }
    public RestQueryConfig getAuthSchema() { return authQueryConfig; }

    public void setServletRequest(HttpServletRequest request) { this.request = request; }

    public String getRestUrl() {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        String codePath = String.format("/rest/read-only/%s/%s", BaseRestController.META_COMMUNICATION_CONTENT_TYPE, rest);
        if (request == null)
            return codePath;
        return UrlHelper.getBaseUrl(request)+codePath;
    }

    public String getRestInsertUrl() {
        if (insertQueryConfig == null) return "";

        String rest = String.format("%s/%s", insertQueryConfig.getServiceName(), insertQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestUpdateUrl() {
        if (updateQueryConfig == null) return "";

        String rest = String.format("%s/%s", updateQueryConfig.getServiceName(), updateQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestDeleteUrl() {
        if (deleteQueryConfig == null) return "";

        String rest = String.format("%s/%s", deleteQueryConfig.getServiceName(), deleteQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestValidateUrl() {
        if (validateQueryConfig == null) return "";

        String rest = String.format("%s/%s", validateQueryConfig.getServiceName(), validateQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestErrorUrl() {
        if (errorQueryConfig == null) return "";

        String rest = String.format("%s/%s", errorQueryConfig.getServiceName(), errorQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestImportUrl() {
        if (importQueryConfig == null) return "";

        String rest = String.format("%s/%s", importQueryConfig.getServiceName(), importQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestValidationErrorXlsxUrl() {
        if (errorQueryConfig == null) return "";

        String rest = String.format("%s/%s", errorQueryConfig.getServiceName(), errorQueryConfig.getActionName());
        return String.format("%s/rest/read-only/xlsx/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestExportXlsxUrl() {
        if (exportQueryConfig == null) return "";

        String rest = String.format("%s/%s", exportQueryConfig.getServiceName(), exportQueryConfig.getActionName());
        return String.format("%s/rest/read-only/xlsx/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestRoleUrl() {
        if (roleQueryConfig == null) return "";

        String rest = String.format("%s/%s", roleQueryConfig.getServiceName(), roleQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestAuthUrl() {
        if (authQueryConfig == null) return "";

        String rest = String.format("%s/%s", authQueryConfig.getServiceName(), authQueryConfig.getActionName());
        return String.format("%s/rest/read-only/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestCountUrl() {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        return String.format("%s/rest/count/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestXlsxUrl() {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        return String.format("%s/rest/read-only/xlsx/%s", UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestXlsUrl () {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        return String.format("%s/rest/read-only/xls/%s" , UrlHelper.getBaseUrl(request), rest);
    }

    public String getRestXmlUrl () {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        return String.format("%s/rest/read-only/xml/%s" , UrlHelper.getBaseUrl(request), rest);
    }

    public Map<String, String> getRestSelectUrls() {
        Map<String, String> result = new HashMap<>();
        for (RestQueryConfig.FieldConfig field : queryConfig.getFields()) {
            if (field.getType() != BaseMessageColumn.ColumnType.SELECT &&
                field.getType() != BaseMessageColumn.ColumnType.SINGLE_SELECT &&
                field.getType() != BaseMessageColumn.ColumnType.SUBLIST) continue;

            RestQueryConfig sc = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(field.getSelectQuery(), null);
            if (sc != null) {
                String url = String.format("%s/%s", sc.getServiceName(), sc.getActionName());
                url = String.format("%s/rest/read-only/json/%s" , UrlHelper.getBaseUrl(request), url);
                result.put(field.getId()+"_url", url);
            }
        }
        return result;
    }

    public Map<String, RestQueryConfig> getRestSelectSchemas() {
        Map<String, RestQueryConfig> result = new HashMap();
        for (RestQueryConfig.FieldConfig field : queryConfig.getFields()) {
            if (field.getType() != BaseMessageColumn.ColumnType.SELECT &&
                    field.getType() != BaseMessageColumn.ColumnType.SINGLE_SELECT &&
                    field.getType() != BaseMessageColumn.ColumnType.SUBLIST) continue;

            RestQueryConfig sc = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(field.getSelectQuery(), null);
            if (sc != null)
                result.put("schema_"+field.getId(), sc);
        }
        return result;
    }

    public String getRestBaseUrl() {
        return String.format("%s/rest/", UrlHelper.getBaseUrl(request));
    }

    public ArrayList<String > getExcludeCols () { return exclude; }
    public ArrayList<String		 > getExport () { return export ; }
    public ArrayList<SelectConfig> getSelects() { return selects; }
    public ArrayList<GroupConfig>  getGroups () { return groups ; }
    public ArrayList<LinkConfig  > getLinks  () { return links  ; }
    public ArrayList<ExpandConfig> getExpands() { return expands; }
    public ArrayList<JavaScriptValidationConfig> getJsValidations() { return jsValidations; }

    public void configure(Node node) {
        id    = XmlNodeHelper.getAttributeValue(node, "id"   );
        name  = XmlNodeHelper.getAttributeValue(node, "name" );
        query = XmlNodeHelper.getAttributeValue(node, "rest-query");

        if (id.isEmpty() || name.isEmpty() || query.isEmpty())
            throw new NullPointerException("Attributes 'id/name/rest-query' are mandatory in xml-report");

        queryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(query, null);
        if (queryConfig == null)
            throw new NullPointerException("Invalid REST query reference in the attribute rest-query");

        String e = XmlNodeHelper.getAttributeValue(node, "export");
        if (!e.isEmpty())
            export = new ArrayList<>(Arrays.asList(e.split(",")));

        e = XmlNodeHelper.getAttributeValue(node, "hide-export");
        if (!e.isEmpty())
            hideExport = XmlNodeHelper.getAttributeValueAsBoolean(node, "hide-export");

        e = XmlNodeHelper.getAttributeValue(node, "hide-filter");
        if (!e.isEmpty())
            hideFilter = XmlNodeHelper.getAttributeValueAsBoolean(node, "hide-filter");

        e = XmlNodeHelper.getAttributeValue(node, "hide-pager");
        if (!e.isEmpty())
            hidePager = XmlNodeHelper.getAttributeValueAsBoolean(node, "hide-pager");

        e = XmlNodeHelper.getAttributeValue(node, "pagination");
        if (!e.isEmpty())
            pagination = XmlNodeHelper.getNodeAttributeValueAsEnum(PaginationAt.class, node, "pagination");

        e = XmlNodeHelper.getAttributeValue(node, "filter-type");
        if (!e.isEmpty())
            filterType = XmlNodeHelper.getNodeAttributeValueAsEnum(FilterType.class, node, "filter-type");

        Node excludeNode = XmlNodeHelper.getChildNode(node, "exclude-rest-columns");
        if (excludeNode != null) {
            e = XmlNodeHelper.getNodeValue(excludeNode);
            exclude = new ArrayList<>(Arrays.asList(e.replaceAll("[\r\n\t]*", "").trim().split(",")));
        }

        Node pSizes = XmlNodeHelper.getChildNode(node, "page-sizes");
        if (pSizes != null) {
            e = XmlNodeHelper.getNodeValue(pSizes);
            String[] values = e.replaceAll("[\r\n\t]*", "").trim().split(",");
            sizes = Arrays.asList(values).stream().mapToInt(Integer::parseInt).toArray();
        }

        Node crud = XmlNodeHelper.getChildNode(node, "insert-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            insertQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "update-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            updateQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "delete-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            deleteQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "validate-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            validateQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "error-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            errorQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "export-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            exportQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "import-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            importQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "role-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            roleQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }
        crud = XmlNodeHelper.getChildNode(node, "auth-rest-query");
        if (crud != null) {
            String value = XmlNodeHelper.getNodeValue(crud);
            authQueryConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(value, null);
        }

        ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "select");
        selects = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            selects.add(new SelectConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "group");
        groups = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            groups.add(new GroupConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "link");
        links = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            links.add(new LinkConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "expand");
        expands = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            expands.add(new ExpandConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "javascript-validation");
        jsValidations = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            jsValidations.add(new JavaScriptValidationConfig(nodes.get(idx)));
    }

    public class GroupConfig extends BaseConfig {
        private List<String> fields = new ArrayList<>();

        public List<String> getFields() { return fields; }

        public GroupConfig(Node node) {
            super(node);

            String f = XmlNodeHelper.getAttributeValue(node, "fields");
            fields.addAll(Arrays.asList(f.split(",")));
        }
    }

    public class SelectConfig extends BaseConfig {
        private boolean multiple = false;
        private String url = "";

        public boolean getIsMultiple() { return multiple; }
        public String  getSelectUrl () { return url; }

        public SelectConfig(Node node) {
            super(node);

            multiple = XmlNodeHelper.getAttributeValueAsBoolean(node, "multiple");
            url = XmlNodeHelper.getAttributeValue(node, "url");
        }
    }

    public class LinkConfig extends BaseConfig {
        public LinkConfig(Node node) {
            super(node);
        }
    }

    public class ExpandConfig extends BaseConfig {
        private String parent = "";

        public String getParentReport() { return parent; }

        public ExpandConfig(Node node) {
            super(node);

            parent = XmlNodeHelper.getAttributeValue(node, "parent-report");
            if (parent.isEmpty())
                throw new NullPointerException("Attributes 'parent' is mandatory in expand");
        }
    }

    public enum JavascriptValidationType { MANDATORY, LOGICAL_CONDITION, DATATYPE }
    public enum LogicalValidationType    { VALUE_BASED, FIELD_REF_BASED }

    public class JavaScriptValidationConfig {
        private JavascriptValidationType     type                = JavascriptValidationType.MANDATORY;
        private LogicalValidationType        rangeValidationType = LogicalValidationType.VALUE_BASED;
        private BaseMessageColumn.ColumnType valueType           = BaseMessageColumn.ColumnType.TEXT;
        private QueryFilter.FilterOperator   operator            = QueryFilter.FilterOperator.EQUAL;
        private String[] fields   ;
        private String   condition;
        private String value1, value2;
        private String errorRecord, errorMsg;

        public JavascriptValidationType     getType     () { return type     ; }
        public QueryFilter.FilterOperator   getOperator () { return operator ; }
        public BaseMessageColumn.ColumnType getValueType() { return valueType; }
        public LogicalValidationType getLogicalValidationType() { return rangeValidationType; }
        public String[] getFields   () { return fields   ; }
        public String   getCondition() { return condition; }
        public String   getValue1   () { return value1   ; }
        public String   getValue2   () { return value2   ; }
        public String   getErrorMsg () { return errorMsg ; }
        public String getErrorRecord() { return errorRecord; }

        public JavaScriptValidationConfig(Node node) {
            String value = XmlNodeHelper.getAttributeValue(node, "type");
            if (!value.isEmpty())
                type = XmlNodeHelper.getNodeAttributeValueAsEnum(JavascriptValidationType.class, node, "type");

            value = XmlNodeHelper.getAttributeValue(node, "operator");
            if (!value.isEmpty())
                operator = XmlNodeHelper.getNodeAttributeValueAsEnum(QueryFilter.FilterOperator.class, node, "operator");

            fields    = XmlNodeHelper.getAttributeValue(node, "fields").split(",");
            condition = XmlNodeHelper.getAttributeValue(node, "condition");
            value1    = XmlNodeHelper.getAttributeValue(node, "value1");
            value2    = XmlNodeHelper.getAttributeValue(node, "value2");
            errorMsg  = XmlNodeHelper.getAttributeValue(node, "error-msg");
            errorRecord = XmlNodeHelper.getAttributeValue(node, "error-record");

            value     = XmlNodeHelper.getAttributeValue(node, "value-type");
            if (!value.isEmpty())
                valueType = XmlNodeHelper.getNodeAttributeValueAsEnum(BaseMessageColumn.ColumnType.class, node, "value-type");

            value = XmlNodeHelper.getAttributeValue(node, "logical-type");
            if (!value.isEmpty())
                rangeValidationType = XmlNodeHelper.getNodeAttributeValueAsEnum(LogicalValidationType.class, node, "logical-type");
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", id, name, query);
    }
}
