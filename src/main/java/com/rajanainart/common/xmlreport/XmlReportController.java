package com.rajanainart.common.xmlreport;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;

import com.rajanainart.common.config.AppConfig;
import com.rajanainart.common.rest.BaseRestController;
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
    public final static Map<String, XmlReportConfig> XML_REPORT_CONFIGS = AppConfig.getBeansFromConfig("/xml-report-framework/xml-report-config", "xml-report-config", "id");

    @RequestMapping(value = "/{name:[a-zA-Z0-9]*}", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public ResponseEntity<XmlReportConfig> executeRestQuery(@PathVariable("name") String reportName, HttpServletRequest request) {
        HttpHeaders headers = buildHttpHeaders(XmlReportConfig.META_COMMUNICATION_CONTENT_TYPE);
        String      encoded = HtmlUtils.htmlEscape(reportName);
        if (XML_REPORT_CONFIGS.containsKey(encoded)) {
            XmlReportConfig config = XML_REPORT_CONFIGS.get(encoded);
            config.setServletRequest(request);
            return new ResponseEntity<>(config, headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(new XmlReportConfig(), headers, HttpStatus.BAD_REQUEST);
    }
}
