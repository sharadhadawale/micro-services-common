package com.rajanainart.common.xmlreport;

import java.util.ArrayList;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;

import com.rajanainart.common.config.BaseConfig;
import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.helper.UrlHelper;
import com.rajanainart.common.helper.XmlNodeHelper;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

@Component("xml-report-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XmlReportConfig implements XmlConfig {

    public enum PaginationAt { CLIENT, SERVER }

    public static final String META_COMMUNICATION_CONTENT_TYPE = "json";

    private String id    = "";
    private String name  = "";
    private String query = "";

    private RestQueryConfig queryConfig;

    private boolean hideFilter = false;
    private boolean hideExport = false;
    private boolean hidePager  = false;

    private HttpServletRequest request;
    private PaginationAt       pagination   = PaginationAt.CLIENT;

    private ArrayList<String	  > export  = new ArrayList<>();
    private ArrayList<String	  > exclude = new ArrayList<>();
    private ArrayList<SelectConfig> selects = new ArrayList<>();
    private ArrayList<LinkConfig  > links   = new ArrayList<>();
    private ArrayList<ExpandConfig> expands = new ArrayList<>();
    private int[] sizes;

    public String getId   () { return id;    }
    public String getName () { return name;  }
    public int[]  getPageSizes() { return sizes; }
    public String getRestQuery() { return query; }

    public boolean getHideFilter() { return hideFilter; }
    public boolean getHideExport() { return hideExport; }
    public boolean getHidePager () { return hidePager ; }
    public RestQueryConfig getSchema    () { return queryConfig; }
    public PaginationAt    getPagination() { return pagination ; }

    public void setServletRequest(HttpServletRequest request) { this.request = request; }

    public String getRestUrl() {
        String rest = String.format("%s/%s", queryConfig.getServiceName(), queryConfig.getActionName());
        String codePath = String.format("/rest/read-only/%s/%s", META_COMMUNICATION_CONTENT_TYPE, rest);
        if (request == null)
            return codePath;
        return UrlHelper.getBaseUrl(request)+codePath;
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

    public String getRestBaseUrl() {
        return String.format("%s/rest/", UrlHelper.getBaseUrl(request));
    }

    public ArrayList<String > getExcludeCols () { return exclude; }
    public ArrayList<String		 > getExport () { return export ; }
    public ArrayList<SelectConfig> getSelects() { return selects; }
    public ArrayList<LinkConfig  > getLinks  () { return links  ; }
    public ArrayList<ExpandConfig> getExpands() { return expands; }

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

        ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "select");
        selects = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            selects.add(new SelectConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "link");
        links = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            links.add(new LinkConfig(nodes.get(idx)));

        nodes = XmlNodeHelper.getChildNodes(node, "expand");
        expands = new ArrayList<>();
        for (int idx=0; idx<nodes.size(); idx++)
            expands.add(new ExpandConfig(nodes.get(idx)));
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
}
