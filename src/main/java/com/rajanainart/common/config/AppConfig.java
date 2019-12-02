package com.rajanainart.common.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.helper.EnvironmentHelper;
import com.rajanainart.common.helper.FileHelper;
import com.rajanainart.common.helper.XmlNodeHelper;
import com.rajanainart.common.property.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

public final class AppConfig {

    private AppConfig() {}
	
	private static final Logger log = LoggerFactory.getLogger(AppConfig.class);

    private static ArrayList<String> resourceNames = new ArrayList<>();

    public static final String APP_PROPERTY_FILE = "bootstrap.properties";
    public static final String CONFIG_FILE_EXT   = "config-%s.xml";
    public static final String CONFIG_FOLDER     = "APP_CONFIG_BASE";

    public static String getResourceFilePath() { return APP_PROPERTY_FILE; }

    static {
        String path      = FileHelper.getAppBasePath(getResourceFilePath());
        if (path.isEmpty())
            path         = PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.APP_CONFIG_BASE);
        if (EnvironmentHelper.getValueAsString("APP_TEST", "false").equalsIgnoreCase("true"))
            path      	 = PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.APP_CONFIG_BASE_TEST);
        String seperator = PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.APP_PATH_SEPERATOR);
        if (path.isEmpty())
            path = EnvironmentHelper.getValueAsString(CONFIG_FOLDER, EnvironmentHelper.getHomePath()+seperator+"app_config_base"+seperator);
        String ext = String.format(CONFIG_FILE_EXT, PropertyUtil.getPropertyValue(PropertyUtil.PropertyType.APP_CONFIG_ENV));
        for (String name : new FileHelper(path, ext).getAvailableFiles(true))
            resourceNames.add(name);
        ext = String.format(CONFIG_FILE_EXT, "cmn");
        for (String name : new FileHelper(path, ext).getAvailableFiles(true))
            resourceNames.add(name);
    }

    public static List<String> getResourceNames() {
        return resourceNames;
    }

    @SuppressWarnings("unchecked")
    public static <T extends XmlConfig> Map<String, T> getBeansFromConfig(String query, String configBeanName, String keyAttribute) {
        Map<String, T> list = new HashMap<>();
        for (String name : getResourceNames()) {
            try {
                NodeList nodes = getNodeList(name, query);
                for (int index=0; index<nodes.getLength(); index++) {
                    XmlConfig config = AppContext.getBeanOfType(XmlConfig.class, configBeanName);
                    config.configure(nodes.item(index));
                    list.put(XmlNodeHelper.getAttributeValue(nodes.item(index), keyAttribute), (T)config);
                }
            }
            catch(Exception e) {
                log.info(String.format("Error occurred while processing xml configuration:%s", e.getMessage()));
                log.info(String.format("Query:%s, configBeanName:%s", query, configBeanName));
                e.printStackTrace();
            }
        }
        return list;
    }

    public static NodeList getNodeList(String xmlPath, String query) throws ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Document document = XmlNodeHelper.buildXmlDocumentFromFilePath(xmlPath);
        return XmlNodeHelper.queryDocumentForNodes(document, query);
    }
}
