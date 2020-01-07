package com.rajanainart.common.rest;

import java.util.Map;

import com.rajanainart.common.config.AppConfig;
import com.rajanainart.common.config.AppContext;
import org.springframework.http.HttpHeaders;

import com.rajanainart.common.rest.validator.BaseRestValidator;

public abstract class BaseRestController {

    public final static Map<String, RestQueryConfig  > REST_QUERY_CONFIGS   = AppConfig.getBeansFromConfig("/rest-query-framework/rest-query-config", "rest-query-config", "id");
    public final static Map<String, BaseRestValidator> REST_DATA_VALIDATORS = AppContext.getBeansOfType(BaseRestValidator.class);

    public enum HttpContentType {
        JSON("application/json"),
        XML ("application/xml" ),
        CSV ("application/csv" ),
        TXT ("application/text"),
        TEXT("application/text"),
        XLS ("application/vnd.ms-excel"),
        XLSX("application/vnd.ms-excel");

        private final String text;

        HttpContentType(final String text) { this.text = text; }

        @Override
        public String toString() { return text; }
    }

    public enum HttpContentDisposition {
        JSON("attachment;filename=RestReport.json"),
        XML ("attachment;filename=RestReport.xml" ),
        CSV ("attachment;filename=RestReport.csv" ),
        TXT ("attachment;filename=RestReport.txt" ),
        TEXT("attachment;filename=RestReport.txt" ),
        XLS ("attachment;filename=RestReport.xls" ),
        XLSX("attachment;filename=RestReport.xlsx");

        private final String text;

        HttpContentDisposition(final String text) { this.text = text; }

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
            case XLS:
                headers.add(HttpHeaders.CONTENT_TYPE, HttpContentType.XLS.toString());
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=RestReport.xls");
                break;
            case XLSX:
                headers.add(HttpHeaders.CONTENT_TYPE, HttpContentType.XLSX.toString());
                headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=RestReport.xlsx");
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
