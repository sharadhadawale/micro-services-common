package com.rajanainart.common.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.data.QueryExecutor;
import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.upload.ExcelDocument;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/rest")
public class RestController extends BaseRestController {
    private static final Logger logger = LogManager.getLogger(RestController.class);
    public final static Map<String, Class<BaseEntity>> BASE_ENTITY_TYPES = AppContext.getClassTypesOf(BaseEntity.class);

    @RequestMapping(value = "/meta/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RestQueryConfig> executeRestMetaQuery(@PathVariable("service") String serviceName, @PathVariable("action") String actionName) {
        RestQueryConfig config = getRestQueryConfig(serviceName, actionName);
        HttpHeaders headers    = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        return new ResponseEntity<>(config, headers, HttpStatus.OK);
    }

    @RequestMapping(value = {
                "/read-only/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}",
                "/read-only/{type:^json|xml$}/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}"
            },
            method   = RequestMethod.POST,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> executeRestReadOnlyQuery(@PathVariable(name = "type", required = false) String type,
                                                                              @PathVariable("service") String serviceName,
                                                                              @PathVariable("action" ) String actionName ,
                                                                              @RequestBody RestQueryRequest body) {
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        List<Map<String, Object>> result = new ArrayList<>();
        RestQueryConfig config = getRestQueryConfig(escapedServiceName, escapedActionName);
        HttpHeaders headers    = buildHttpHeaders(type != null && !type.isEmpty() ? type : RestQueryConfig.RestQueryContentType.JSON.toString());
        String     description = "description", status = "status";
        if (config == null) {
            Map<String, Object> m = new HashMap<>();
            m.put(description, String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s' ", escapedServiceName, escapedActionName));
            m.put(status     , RestMessageEntity.MessageStatus.FAILURE.toString());
            result.add(m);
            return new ResponseEntity<>(result, headers, HttpStatus.BAD_REQUEST);
        }
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML) {
            Map<String, Object> m = new HashMap<>();
            m.put(description, "DML REST query is not supported in read-only ");
            m.put(status     , RestMessageEntity.MessageStatus.FAILURE.toString());
            result.add(m);
            return new ResponseEntity<>(result, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try (Database db = new Database(config.getSourceDb())) {
            BaseEntityHandler process = new DefaultEntityHandler();
            process.setup(null, config, body, db);
            String msg = process.preValidateRestEntity();

            if (msg.equals(SUCCESS)) {
                QueryExecutor executor = new QueryExecutor(config, body, db);
                result.addAll(executor.selectAsMapList());
                return new ResponseEntity<>(result, headers, HttpStatus.OK);
            }
            if (!msg.startsWith(SUCCESS) && !msg.isEmpty()) {
                Map<String, Object> m = new HashMap<>();
                m.put(description, msg);
                m.put(status     , RestMessageEntity.MessageStatus.FAILURE.toString());
                result.add(m);
                return new ResponseEntity<>(result, headers, HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    @RequestMapping(value = "/read-only/{type:^xls|xlsx$}/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<Resource> executeResourceRestReadOnlyQuery(@PathVariable(name = "type", required = false) String type,
                                                                     @PathVariable("service") String serviceName,
                                                                     @PathVariable("action" ) String actionName ,
                                                                     @RequestBody RestQueryRequest body,
                                                                     HttpServletResponse response) {
        response.setContentType("application/vnd.ms-excel");

        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        String escapedType        = HtmlUtils.htmlEscape(type       );
        String fileName           = "RestReport."+(escapedType.equalsIgnoreCase("xlsx")?"xlsx":"xls");
        List<Map<String, Object>> result = new ArrayList<>();
        RestQueryConfig config = getRestQueryConfig(escapedServiceName, escapedActionName);
        if (config == null) {
            String m = String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s' ", escapedServiceName, escapedActionName);
            logger.info(m);
            writeExcelMessage(response, m, escapedActionName+"."+escapedType);
            response.setHeader("Content-Disposition", "attachment; filename="+fileName);
            return null;
        }

        response.setHeader("Content-Disposition", "attachment; filename="+fileName);
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML) {
            logger.info("DML REST query is not supported in read-only");
            writeExcelMessage(response, "DML REST query is not supported in read-only", config.getId()+"."+escapedType);
            return null;
        }

        try (Database db = new Database(config.getSourceDb())) {
            BaseEntityHandler process = new DefaultEntityHandler();
            process.setup(null, config, body, db);
            String msg = process.preValidateRestEntity();

            if (msg.equals(SUCCESS)) {
                QueryExecutor executor = new QueryExecutor(config, body, db);
                result.addAll(executor.selectAsMapList());
                try {
                    ExcelDocument document = new ExcelDocument(response.getOutputStream(), config, result, config.getId()+"."+type);
                    document.close();
                }
                catch(IOException io) {
                    io.printStackTrace();
                }
            }
            if (!msg.startsWith(SUCCESS) && !msg.isEmpty()) {
                logger.info(msg);
                writeExcelMessage(response, msg, config.getId()+"."+type);

                return null;
            }
        }
        return null;
    }

    private void writeExcelMessage(ServletResponse response, String message, String fileName) {
        try {
            ExcelDocument document = new ExcelDocument(response.getOutputStream(), message, fileName);
            document.close();
        }
        catch (IOException io) {
            io.printStackTrace();
        }
    }

    @RequestMapping(value = "/bulk/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeRestQueryMultiple(@PathVariable("service") String serviceName,
                                                                     @PathVariable("action" ) String actionName ,
                                                                     @RequestBody List<RestQueryRequest> body) {
        Map<String, BaseEntityHandler> handlers = AppContext.getBeansOfType(BaseEntityHandler.class);
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config  = getRestQueryConfig(serviceName, actionName);
        HttpHeaders     headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.SELECT)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Bulk REST query does not support SELECT, Service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);
        if (!BASE_ENTITY_TYPES.containsKey(config.getEntityName()))
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("BaseRestEntity '%s' does not exist", config.getEntityName()), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);

        List<BaseEntity> results = new ArrayList<>();
        String prefix = "restentityhandler";
        String skey   = "%s-%s";
        BaseEntityHandler process = handlers.containsKey(String.format(skey, prefix, escapedServiceName)) ?
                                            handlers.get(String.format(skey, prefix, escapedServiceName)) :
                                            handlers.get(String.format("%s-default", prefix));

        int success = 0;
        try (Database db = new Database()) {
            for (RestQueryRequest request : body) {
                process.setup(BASE_ENTITY_TYPES.get(config.getEntityName()), config, request, db);
                String msg = process.preValidateRestEntity();
                if (msg.equals(SUCCESS) ||
                        (msg.startsWith(SUCCESS) && msg.length() > SUCCESS.length())) {
                    StringBuilder message   = new StringBuilder();
                    List<BaseEntity> result = process.executeQuery(message);
                    results.addAll(result);
                    for (BaseEntity entity : result) {
                        RestMessageEntity m = (RestMessageEntity)entity;
                        if (m != null && m.getStatus() == RestMessageEntity.MessageStatus.SUCCESS) success++;
                    }
                }
                else if (!msg.startsWith(SUCCESS) && !msg.isEmpty()) {
                    List<BaseEntity> result = RestMessageEntity.getInstanceList("", msg, RestMessageEntity.MessageStatus.FAILURE);
                    results.addAll(result);
                }
            }
            if (success == results.size()) db.commit();
        }
        if (success == results.size()) {
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("%s record(s) updated", results.size()), RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
        }
        if (results != null && results.size() == 0)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "No records updated", RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
        return new ResponseEntity<>(results, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeRestQuery(@PathVariable("service") String serviceName, @PathVariable("action") String actionName,
                                                             @RequestBody RestQueryRequest body) {
        Map<String, BaseEntityHandler> handlers = AppContext.getBeansOfType(BaseEntityHandler.class);
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config    = getRestQueryConfig(serviceName, actionName);
        HttpHeaders     headers   = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);

        List<BaseEntity> results = new ArrayList<>();
        String prefix = "restentityhandler";
        String skey   = "%s-%s";
        BaseEntityHandler process = handlers.containsKey(String.format(skey, prefix, escapedServiceName)) ?
                                            handlers.get(String.format(skey, prefix, escapedServiceName)) :
                                            handlers.get(String.format("%s-default", prefix));

        try (Database db = new Database()) {
            process.setup(BASE_ENTITY_TYPES.get(config.getEntityName()), config, body, db);
            String msg = process.preValidateRestEntity();
            if (msg.equals(SUCCESS)) {
                if (!BASE_ENTITY_TYPES.containsKey(config.getEntityName()))
                    return new ResponseEntity<>(
                            RestMessageEntity.getInstanceList("", String.format("BaseRestEntity '%s' does not exist", config.getEntityName()), RestMessageEntity.MessageStatus.FAILURE),
                            headers, HttpStatus.INTERNAL_SERVER_ERROR);
                StringBuilder message = new StringBuilder();
                if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML) {
                    List<BaseEntity> result = process.executeQuery(message);
                    db.commit();
                    return new ResponseEntity<>(result, headers, message.toString().startsWith(SUCCESS) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
                }
                else {
                    results = process.fetchRestQueryResultSet(BASE_ENTITY_TYPES.get(config.getEntityName()), message);
                    msg     = message.toString().isEmpty() ? process.postValidateRestEntity(results) : message.toString();
                }
            }
            if (msg.startsWith(SUCCESS) && msg.length() > SUCCESS.length())
                return new ResponseEntity<>(
                        RestMessageEntity.getInstanceList("", msg.replace(SUCCESS +":", "")), headers, HttpStatus.OK);
            else if (!msg.startsWith(SUCCESS) && !msg.isEmpty())
                return new ResponseEntity<>(
                        RestMessageEntity.getInstanceList("", msg, RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (results != null && results.size() == 0)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "No records found", RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
        return new ResponseEntity<>(results, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/**", method = { RequestMethod.POST })
    @ResponseBody
    public ResponseEntity<BaseEntity> defaultQuery() {
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        return new ResponseEntity<>(RestMessageEntity.getInstance("", "Invalid REST query", RestMessageEntity.MessageStatus.FAILURE),
                                    headers, HttpStatus.BAD_REQUEST);
    }

    @RequestMapping(value = "/**", method = { RequestMethod.GET })
    @ResponseBody
    public ResponseEntity<BaseEntity> defaultQuery1() {
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        return new ResponseEntity<>(RestMessageEntity.getInstance("", "Invalid REST query", RestMessageEntity.MessageStatus.FAILURE),
                headers, HttpStatus.BAD_REQUEST);
    }

}
