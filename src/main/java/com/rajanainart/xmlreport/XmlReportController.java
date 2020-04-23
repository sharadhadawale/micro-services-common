package com.rajanainart.xmlreport;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.rajanainart.config.AppConfig;
import com.rajanainart.rest.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

@Component
@RequestMapping("/xml-report")
public class XmlReportController extends BaseRestController {
    public final static Map<String, XmlReportConfig     > XML_REPORT_CONFIGS       = AppConfig.getBeansFromConfig("/xml-report-framework/xml-report-config", "xml-report-config", "id");
    public final static Map<String, XmlReportGroupConfig> XML_REPORT_GROUP_CONFIGS = AppConfig.getBeansFromConfig("/xml-report-framework/xml-report-group-config", "xml-report-group-config", "id");

    @RequestMapping(value = "/{name:[a-zA-Z0-9]*}", method = {RequestMethod.POST, RequestMethod.GET})
    @ResponseBody
    public ResponseEntity<XmlReportConfig> executeRestQuery(@PathVariable("name") String reportName, HttpServletRequest request) {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        String      encoded = HtmlUtils.htmlEscape(reportName);
        if (XML_REPORT_CONFIGS.containsKey(encoded)) {
            XmlReportConfig config = XML_REPORT_CONFIGS.get(encoded);
            config.setServletRequest(request);
            return new ResponseEntity<>(config, headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(new XmlReportConfig(), headers, HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/group/{name:[a-zA-Z0-9]*}", method = {RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<XmlReportGroupConfig> executeGroupRestQuery(@PathVariable("name") String reportName, HttpServletRequest request) {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        String      encoded = HtmlUtils.htmlEscape(reportName);
        if (XML_REPORT_GROUP_CONFIGS.containsKey(encoded)) {
            XmlReportGroupConfig config = XML_REPORT_GROUP_CONFIGS.get(encoded);
            return new ResponseEntity<>(config, headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(new XmlReportGroupConfig(), headers, HttpStatus.BAD_REQUEST);
    }
}
