package com.rajanainart.common.template;

import com.rajanainart.common.rest.BaseRestController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Component
@RequestMapping("/template-ext")
public class TemplateController extends BaseRestController {
    @RequestMapping(value = "/expression-parts", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<ExpressionPart>> getExpressionParts() {
        HttpHeaders headers = buildHttpHeaders("json");
        return new ResponseEntity<>(Expression.getExpressionParts(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/operators", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String[]> getOperators() {
        HttpHeaders headers = buildHttpHeaders("json");
        return new ResponseEntity<>(Expression.OPERATORS, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/methods", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<String>> getMethods() {
        HttpHeaders headers = buildHttpHeaders("json");
        return new ResponseEntity<>(Expression.getSystemMethods(), headers, HttpStatus.OK);
    }
}
