package com.rajanainart.common.rest;

import java.util.Map;

import com.rajanainart.common.config.AppConfig;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.rest.validator.BaseRestValidator;
import org.springframework.http.HttpHeaders;

public abstract class BaseRestController {

    public final static Map<String, RestQueryConfig  > REST_QUERY_CONFIGS   = AppConfig.getBeansFromConfig("/rest-query-framework/rest-query-config", "rest-query-config", "id");
    public final static Map<String, BaseRestValidator> REST_DATA_VALIDATORS = AppContext.getBeansOfType(BaseRestValidator.class);

    public enum HttpContentType {
        JSON("application/json"),
        XML ("application/xml" ),
        CSV ("application/csv" ),
        TEXT("application/text");

        private final String text;

        HttpContentType(final String text) { this.text = text; }

        @Override
        public String toString() { return text; }
    }

    public final static String SUCCESS = "SUCCESS";
    public final static String ERROR   = "ERROR";

    public HttpHeaders buildHttpHeaders(String contentType) {
        HttpHeaders headers = new HttpHeaders();
        RestQueryConfig.RestQueryContentType type = Enum.valueOf(RestQueryConfig.RestQueryContentType.class, contentType.toUpperCase());
        switch (type) {
            case XML:
                headers.add(HttpHeaders.CONTENT_TYPE, HttpContentType.XML.toString());
                break;
            case CSV:
                headers.add(HttpHeaders.CONTENT_TYPE, HttpContentType.CSV.toString());
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=RestReport.csv");
                break;
            default:
                headers.add(HttpHeaders.CONTENT_TYPE, HttpContentType.JSON.toString());
                break;
        }
        return headers;
    }

    public RestQueryConfig getRestQueryConfig(String service, String action) {
        for (Map.Entry<String, RestQueryConfig> entry : REST_QUERY_CONFIGS.entrySet())
            if (entry.getValue().getServiceName().equalsIgnoreCase(service) && entry.getValue().getActionName().equalsIgnoreCase(action))
                return entry.getValue();
        return null;
    }
}
