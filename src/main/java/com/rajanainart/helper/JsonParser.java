package com.rajanainart.helper;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class JsonParser {
    private String json  ;
    private Map    allMap;

    public Map getAllMap() { return allMap; }

    public JsonParser(String json) throws IOException {
        this.json = json;

        ObjectMapper mapper = new ObjectMapper();
        allMap = mapper.readValue(json, Map.class);
    }

    public List getValueAsList(String propertyName) {
        return (List)allMap.get(propertyName);
    }

    public Map getValueAsMap(String propertyName) {
        return (Map)allMap.get(propertyName);
    }

    public String getValueAsString(String propertyName) {
        return (String)allMap.get(propertyName);
    }

    public static List getValueAsList(Map map, String propertyName) {
        return (List)map.get(propertyName);
    }

    public static Map getValueAsMap(Map map, String propertyName) {
        return (Map)map.get(propertyName);
    }

    public static String getValueAsString(Map map, String propertyName) {
        return (String)map.get(propertyName);
    }
}
