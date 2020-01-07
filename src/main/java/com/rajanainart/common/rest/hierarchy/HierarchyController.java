package com.rajanainart.common.rest.hierarchy;

import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.rest.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/hierarchy")
public class HierarchyController extends BaseRestController {
    public final static Map<String, Class<BaseHierarchy>> BASE_HIERARCHY_TYPES = AppContext.getClassTypesOf(BaseHierarchy.class);

    @RequestMapping(value = {
                                "/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}",
                                "/{type:^json|xml$}/{service:[a-zA-Z0-9]*}/{action:[a-zA-Z0-9]*}"
                            },
                    method   = RequestMethod.POST,
                    produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> executeHierarchyRestQuery(@PathVariable(name = "type", required = false) String type,
                                                                      @PathVariable("service") String serviceName,
                                                                      @PathVariable("action") String actionName,
                                                                      @RequestBody RestQueryRequest body) {
        String escapedServiceName = HtmlUtils.htmlEscape(serviceName);
        String escapedActionName  = HtmlUtils.htmlEscape(actionName );
        RestQueryConfig config    = getRestQueryConfig(serviceName, actionName);
        HttpHeaders headers       = buildHttpHeaders(type != null && !type.isEmpty() ? type : RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No REST query config is available for the service '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);
        if (config.getRestQueryType() == RestQueryConfig.RestQueryType.DML)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. /hierarchy does not support DML, '%s' and action '%s'", escapedServiceName, escapedActionName), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);
        if (!BASE_HIERARCHY_TYPES.containsKey(config.getEntityName()))
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("BaseHierarchyEntity '%s' does not exist", config.getEntityName()), RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);

        List<BaseEntity>     results = new ArrayList<>();
        DefaultEntityHandler process = new DefaultEntityHandler();

        try (Database db = new Database()) {
            process.setup(BASE_HIERARCHY_TYPES.get(config.getEntityName()), config, body, db);
            String msg = process.preValidateRestEntity();
            if (msg.equals(SUCCESS)) {
                Hierarchy hierarchy = new Hierarchy(process.getQueryExecutor());
                results = hierarchy.buildHierarchyAsBaseEntity(BASE_HIERARCHY_TYPES.get(config.getEntityName()));
                msg     = BaseRestController.SUCCESS;
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
}
