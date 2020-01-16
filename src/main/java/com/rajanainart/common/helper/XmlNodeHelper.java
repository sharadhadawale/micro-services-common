package com.rajanainart.common.helper;

import java.io.InputStream;
import java.util.Date;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public final class XmlNodeHelper {
    private XmlNodeHelper() {}

    private static DocumentBuilderFactory factory = null;
    private static DocumentBuilder        builder = null;
    static {
        factory = DocumentBuilderFactory.newInstance();
        try {
            factory.setFeature("http://xml.org/sax/features/external-general-entities"  , false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl"   , true );
            builder = factory.newDocumentBuilder();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String getAttributeValue(Node node, String attribute) {
        Node n = null;
        if (node.getAttributes() != null)
            n = node.getAttributes().getNamedItem(attribute);
        return n != null ? n.getNodeValue() : "";
    }

    public static boolean getAttributeValueAsBoolean(Node node, String attribute) {
        String value = getAttributeValue(node, attribute);
        return Boolean.valueOf(value);
    }

    public static int getAttributeValueAsInteger(Node node, String attribute) {
        String value = getAttributeValue(node, attribute);
        return !value.isEmpty() ? Integer.valueOf(value) : 0;
    }

    public static double getAttributeValueAsDouble(Node node, String attribute) {
        String value = getAttributeValue(node, attribute);
        return Double.valueOf(value);
    }

    public static String getNodeValue(Node node) {
        return node.getTextContent();
    }

    public static Date getNodeValueAsDate(Node node, String format) {
        String value = getNodeValue(node);
        if (value.isEmpty())
            return MiscHelper.getSystemDateTime();
        return MiscHelper.convertStringToDate(value, format);
    }

    public static boolean getNodeValueAsBoolean(Node node) {
        String value = getNodeValue(node);
        return Boolean.valueOf(value);
    }

    public static int getNodeValueAsInteger(Node node) {
        String value = getNodeValue(node);
        return Integer.valueOf(value);
    }

    public static double getNodeValueAsDouble(Node node) {
        String value = getNodeValue(node);
        return Double.valueOf(value);
    }

    public static <T extends Enum<T>> T getNodeValueAsEnum(Class<T> cls, Node node) {
        return Enum.valueOf(cls, getNodeValue(node).toUpperCase());
    }

    public static <T extends Enum<T>> T getNodeAttributeValueAsEnum(Class<T> cls, Node node, String attribute) {
        return Enum.valueOf(cls, getAttributeValue(node, attribute).toUpperCase());
    }

    public static Node getChildNode(Node node, String nodeName) {
        NodeList children = node.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index).getNodeName().equalsIgnoreCase(nodeName))
                return children.item(index);
        }
        return null;
    }

    public static ArrayList<Node> getChildNodes(Node node, String nodeName) {
        NodeList children = node.getChildNodes();
        ArrayList<Node> result = new ArrayList<>();
        for (int index = 0; index < children.getLength(); index++) {
            if (children.item(index).getNodeName().equalsIgnoreCase(nodeName))
                result.add(children.item(index));
        }
        return result;
    }

    public static Map<String, String> getPropertiesAsMap(Node node, String nodeName, String key, String value) {
        ArrayList<Node> requests = XmlNodeHelper.getChildNodes(node, nodeName);
        Map<String, String> map = new HashMap<String, String>();
        for (int index = 0; index < requests.size(); index++) {
            String k = XmlNodeHelper.getAttributeValue(requests.get(index), key);
            if (!k.isEmpty())
                map.put(k, XmlNodeHelper.getAttributeValue(requests.get(index), value));
        }
        return map;
    }

    public static Document buildXmlDocumentFromString(String xml) throws SAXException, IOException {
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        document.getDocumentElement().normalize();
        return document;
    }

    public static Document buildXmlDocumentFromFilePath(String xmlPath) throws SAXException, IOException {
        File file = new File(xmlPath);
        Document document = builder.parse(file);
        document.getDocumentElement().normalize();
        return document;
    }

    public static Document buildXmlDocumentFromResource(String resourceName) throws SAXException, IOException {
        try (InputStream stream = XmlNodeHelper.class.getClassLoader().getResourceAsStream(resourceName)) {
            Document document = builder.parse(stream);
            document.getDocumentElement().normalize();
            return document;
        }
    }

    public static NodeList queryDocumentForNodes(Document document, String query) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (NodeList) xpath.compile(query).evaluate(document, XPathConstants.NODESET);
    }

    public static String queryDocumentForString(Document document, String query) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        return (String) xpath.compile(query).evaluate(document, XPathConstants.STRING);
    }

    public static String queryDocForString(Document document, String query, NamespaceContext ns)
            throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(ns);
        String temp = (String) xpath.compile(query).evaluate(document, XPathConstants.STRING);
        return temp;
    }

    public static int queryDocumentForInteger(Document document, String query) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Object result = xpath.compile(query).evaluate(document, XPathConstants.NUMBER);
        return (int) Double.parseDouble(result.toString());
    }

    public static double queryDocumentForDouble(Document document, String query) throws XPathExpressionException {
        XPath xpath = XPathFactory.newInstance().newXPath();
        Object result = xpath.compile(query).evaluate(document, XPathConstants.NUMBER);
        return Double.parseDouble(result.toString());
    }
}
