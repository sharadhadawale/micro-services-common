package com.rajanainart.xmlreport;

import com.rajanainart.config.XmlConfig;
import com.rajanainart.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

@Component("xml-report-group-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class XmlReportGroupConfig implements XmlConfig {
    public enum GroupType { TAB }

    private String       id   = "";
    private String       name = "";
    private GroupType    type = GroupType.TAB;
    private List<String> xmlReports;

    public String       getId        () { return id  ; }
    public String       getName      () { return name; }
    public GroupType    getGroupType () { return type; }
    public List<String> getXmlReports() { return xmlReports; }

    public void configure(Node node) {
        id   = XmlNodeHelper.getAttributeValue(node, "id"  );
        name = XmlNodeHelper.getAttributeValue(node, "name");

        if (id.isEmpty() || name.isEmpty())
            throw new NullPointerException("Attributes 'id/name' are mandatory in xml-report-group");

        xmlReports = new ArrayList<>();
        ArrayList<Node> nodes = XmlNodeHelper.getChildNodes(node, "xml-report");
        for (int idx=0; idx<nodes.size(); idx++)
            xmlReports.add(XmlNodeHelper.getNodeValue(nodes.get(idx)));

        if (xmlReports.size() == 0)
            throw new NullPointerException("Atleast one 'xml-report' node is required");

        String e = XmlNodeHelper.getAttributeValue(node, "type");
        if (!e.isEmpty())
            type = XmlNodeHelper.getNodeAttributeValueAsEnum(GroupType.class, node, "type");
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", id, name, type.toString());
    }
}
