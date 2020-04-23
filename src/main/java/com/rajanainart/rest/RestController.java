package com.rajanainart.rest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.cache.CacheException;
import com.rajanainart.cache.CacheItem;
import com.rajanainart.cache.CacheManager;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.config.AppContext;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.integration.IntegrationManager;
import com.rajanainart.resource.BaseResourceWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;
import org.springframework.web.util.HtmlUtils;

import javax.servlet.http.HttpServletResponse;

@Controller
@RequestMapping("/rest")
public class RestController extends BaseRestController {
    private static final Logger logger = LogManager.getLogger(RestController.class);
    public final static Map<String, Class<BaseEntity >> BASE_ENTITY_TYPES = AppContext.getClassTypesOf(BaseEntity.class);

    @RequestMapping(value = "/meta/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<RestQueryConfig> executeRestMetaQuery(@PathVariable("service") String serviceName, @PathVariable("action") String actionName) {
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config = getRestQueryConfig(escapedServiceName, escapedActionName);
        HttpHeaders headers    = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (MiscHelper.isValidName(config.getId()))
            return new ResponseEntity<>(config, headers, HttpStatus.OK);
        return new ResponseEntity<>(null, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/count/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    public ResponseEntity<Long> executeRestCountQuery(@PathVariable("service") String serviceName, @PathVariable("action") String actionName,
                                                      @RequestBody RestQueryRequest body) {
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config = getRestQueryConfig(escapedServiceName, escapedActionName);
        HttpHeaders headers    = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config != null && MiscHelper.isValidName(config.getId())) {
            long count = 0;
            try (QueryExecutor executor = new QueryExecutor(config, body)) {
                count = executor.getTotalRecordCount();
            }
            return new ResponseEntity<>(count, headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(0L, headers, HttpStatus.OK);
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

        CacheManager manager = null;
        if (config.isCacheEnabled()) {
            try {
                manager = new CacheManager(config, body);
                List<Map<String, Object>> cachedResult = manager.getCachedMapRecords();
                if (cachedResult != null) {
                    logger.info(String.format("Resolved from Cache server:%s", config.getId()));
                    return new ResponseEntity<>(cachedResult, headers, HttpStatus.OK);
                }
            }
            catch (CacheException ex) {
                ex.printStackTrace();
            }
        }

        try (Database db = new Database(config.getSourceDb())) {
            BaseEntityHandler process = getEntityHandler(escapedServiceName);
            process.setup(null, config, body, db);
            String msg = process.preValidateRestEntity();

            if (msg.equals(SUCCESS)) {
                result.addAll(process.fetchRestQueryAsMap());
                if (body.getPageSize().orElse(-1) > 0 && body.getCurrentPageNumber() != -1) {
                    if (result.size() == 0) result.add(new HashMap<>());

                    QueryExecutor executor = new QueryExecutor(config, body, db);
                    result.get(0).put("total-records", executor.getTotalRecordCount());
                    result.get(0).put("current-page" , body.getCurrentPageNumber());
                }
                result = process.executePostAction(result);
                if (config.isCacheEnabled() && manager != null) {
                    CacheItem item = manager.saveMapRecords(result);
                    logger.info(String.format("Stored to cache server:%s:%s", config.getId(), item.buildCacheKey()));
                }

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

    @RequestMapping(value = "/read-only/{type:^xls|xlsx|pdf|csv|tsv|txt$}/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public void executeResourceRestReadOnlyQuery(@PathVariable(name = "type", required = false) String type,
                                                                     @PathVariable("service") String serviceName,
                                                                     @PathVariable("action" ) String actionName ,
                                                                     @RequestBody RestQueryRequest body,
                                                                     HttpServletResponse response) {
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        String escapedType        = HtmlUtils.htmlEscape(type       );
        RestQueryConfig config    = getRestQueryConfig(escapedServiceName, escapedActionName);
        String fileName           = String.format("RestResult.%s", escapedType);
        List<Map<String, Object>> result = null;

        String key = String.format("resource-writer-%s", escapedType);
        BaseResourceWriter resourceWriter = IntegrationManager.RESOURCE_WRITERS.getOrDefault(key, IntegrationManager.RESOURCE_WRITERS.get("resource-writer-txt"));

        if (config == null) {
            String msg = String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s' ", escapedServiceName, escapedActionName);
            writeResourceMessage(resourceWriter, null, response, msg, fileName);
            return;
        }

        fileName = String.format("%s.%s", config.getId(), escapedType);
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML) {
            String msg = "DML REST query is not supported in read-only";

            writeResourceMessage(resourceWriter, config, response, msg, fileName);
            return;
        }

        CacheManager manager = null;
        if (config.isCacheEnabled()) {
            try {
                manager = new CacheManager(config, body);
                result  = manager.getCachedMapRecords();
                if (result != null)
                    logger.info(String.format("Resolved from Cache server:%s", config.getId()));
            }
            catch (CacheException ex) {
                ex.printStackTrace();
            }
        }
        if (result == null) {
            try (Database db = new Database(config.getSourceDb())) {
                BaseEntityHandler process = getEntityHandler(escapedServiceName);
                process.setup(null, config, body, db);
                String msg = process.preValidateRestEntity();

                if (msg.equals(SUCCESS)) {
                    result = process.fetchRestQueryAsMap();
                    result = process.executePostAction(result);
                    if (config.isCacheEnabled() && manager != null) {
                        CacheItem item = manager.saveMapRecords(result);
                        logger.info(String.format("Stored to cache server:%s:%s", config.getId(), item.buildCacheKey()));
                    }
                }
                if (!msg.startsWith(SUCCESS) && !msg.isEmpty()) {
                    writeResourceMessage(resourceWriter, config, response, msg, fileName);
                    return;
                }
            }
        }

        try {
            String hide = body.getParams().getOrDefault("HideHeader", "true");
            resourceWriter.init(response, config, fileName);
            if (!hide.equalsIgnoreCase("true"))
                resourceWriter.writeHeader(0, 0);
            resourceWriter.writeContent(result, hide.equalsIgnoreCase("true") ? 0 : 2, 0);
            resourceWriter.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void writeResourceMessage(BaseResourceWriter resourceWriter, RestQueryConfig config,
                                   HttpServletResponse response, String message, String fileName) {
        logger.info(message);
        try {
            resourceWriter.init(response, config, fileName);
            resourceWriter.writeContent(message, 0, 0);
            resourceWriter.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @RequestMapping(value = {
                                "/bulk/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}",
                                "/dml/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}"
                            },
                    method   = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeRestQueryMultiple(@PathVariable("service") String serviceName,
                                                                     @PathVariable("action" ) String actionName ,
                                                                     @RequestBody List<RestQueryRequest> body) {
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
                    RestMessageEntity.getInstanceList("", String.format("Bulk/DML REST query does not support SELECT, Service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);

        List<BaseEntity>  results = new ArrayList<>();
        BaseEntityHandler process = getEntityHandler(escapedServiceName);

        int success = 0;
        try (Database db = new Database()) {
            for (RestQueryRequest request : body) {
                process.setup(null, config, request, db);
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
                    RestMessageEntity.getInstanceList("", String.format("Records have been updated", results.size()), RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
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
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config    = getRestQueryConfig(serviceName, actionName);
        HttpHeaders     headers   = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);
        if (!BASE_ENTITY_TYPES.containsKey(config.getEntityName()))
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("BaseRestEntity '%s' does not exist", config.getEntityName()), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);

        CacheManager manager = null;
        if (config.isCacheEnabled() && config.getRestQueryType() != RestQueryConfig.RestQueryType.DML) {
            try {
                manager = new CacheManager(config, body);
                List<BaseEntity> cachedResult = manager.getCachedEntities();
                if (cachedResult != null) {
                    logger.info("Resolved from Cache server");
                    return new ResponseEntity<>(cachedResult, headers, HttpStatus.OK);
                }
            }
            catch (CacheException ex) {
                ex.printStackTrace();
            }
        }

        List<BaseEntity>  results = new ArrayList<>();
        BaseEntityHandler process = getEntityHandler(escapedServiceName);

        try (Database db = new Database()) {
            process.setup(BASE_ENTITY_TYPES.get(config.getEntityName()), config, body, db);
            String msg = process.preValidateRestEntity();
            if (msg.equals(SUCCESS)) {
                StringBuilder message = new StringBuilder();
                if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML) {
                    List<BaseEntity> result = process.executeQuery(message);
                    db.commit();
                    return new ResponseEntity<>(result, headers, message.toString().startsWith(SUCCESS) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR);
                }
                else {
                    results = process.fetchRestQueryResultSet(BASE_ENTITY_TYPES.get(config.getEntityName()), message);
                    results = process.executePostAction      (BASE_ENTITY_TYPES.get(config.getEntityName()), results);
                    msg     = message.toString().isEmpty() ? process.postValidateRestEntity(results) : message.toString();
                    if (config.isCacheEnabled() && manager != null) {
                        CacheItem item = manager.saveEntityRecords(results);
                        logger.info(String.format("Stored to cache server:%s", item.buildCacheKey()));
                    }
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

    public static BaseEntityHandler getEntityHandler(String escapedServiceName) {
        Map<String, BaseEntityHandler> handlers = AppContext.getBeansOfType(BaseEntityHandler.class);
        String prefix = "restentityhandler";
        String skey   = "%s-%s";
        return  handlers.containsKey(String.format(skey, prefix, escapedServiceName)) ?
                handlers.get(String.format(skey, prefix, escapedServiceName)) :
                handlers.get(String.format("%s-default", prefix));
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
