package com.rajanainart.template;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;
import com.rajanainart.data.QueryExecutor;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.helper.TimeZoneHelper;
import com.rajanainart.rest.BaseRestController;
import com.rajanainart.rest.RestMessageEntity;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.rest.RestQueryRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

import java.time.ZoneId;
import java.util.*;

@Component
@RequestMapping("/template-ext")
public class TemplateController extends BaseRestController {
    @RequestMapping(value = "/expression-parts", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<ExpressionPart>> getExpressionParts() {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        return new ResponseEntity<>(Expression.getExpressionParts(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/operators", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String[]> getOperators() {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        return new ResponseEntity<>(Expression.OPERATORS, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/methods", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<String>> getMethods() {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        return new ResponseEntity<>(Expression.getSystemMethods(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/time-zones", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<String[]> getTimeZones() {
        HttpHeaders headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        return new ResponseEntity<>(TimeZone.getAvailableIDs(), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/time-zones-short", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<List<String>> getTimeZonesShort() {
        HttpHeaders  headers   = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        String[]     timeZones = TimeZone.getAvailableIDs();
        List<String> result    = new ArrayList<>();
        Arrays.asList(timeZones).forEach(x -> {
            if (x.length() == 3)
                result.add(x);
        });
        return new ResponseEntity<>(result, headers, HttpStatus.OK);
    }

    @RequestMapping(value = {
            "/time-zones-ext",
            "/time-zones-ext/{offset:^gmt|utc$}",
            },
            method   = RequestMethod.GET,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    @ResponseBody
    public ResponseEntity<List<TimeZoneHelper.TimeZoneDetail>> getTimeZonesExt(
            ZoneId zoneId, @PathVariable(name = "offset", required = false) String offset) {
        TimeZoneHelper.OffsetBase offsetBase = MiscHelper.convertStringToEnum(TimeZoneHelper.OffsetBase.class,
                                                                offset != null && !offset.isEmpty() ? offset : "UTC",
                                                                TimeZoneHelper.OffsetBase.UTC);
        HttpHeaders    headers = buildHttpHeaders(BaseRestController.META_COMMUNICATION_CONTENT_TYPE);
        TimeZoneHelper helper  = new TimeZoneHelper(offsetBase);
        return new ResponseEntity<>(helper.getTimeZones(zoneId), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/parse/{name:[a-zA-Z0-9]*}", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<List<BaseEntity>> getParsedTemplate(@PathVariable("name") String name,
                                                              @RequestBody RestQueryRequest body) {
        String            escaped = HtmlUtils.htmlEscape(name);
        RestQueryConfig   config  = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(escaped, null);
        HttpHeaders headers       = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        if (config == null)
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", String.format("Invalid Request. No RestQuery config is available '%s'", escaped, RestMessageEntity.MessageStatus.FAILURE)),
                    headers, HttpStatus.BAD_REQUEST);

        String templateId = body.getParams().getOrDefault("ID", "");
        if (!MiscHelper.isNumeric(templateId))
            return new ResponseEntity<>(
                    RestMessageEntity.getInstanceList("", "Invalid 'ID' request parameter", RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.BAD_REQUEST);

        return parseTemplate(config, body, headers, templateId);
    }

    private ResponseEntity<List<BaseEntity>> parseTemplate(RestQueryConfig config, RestQueryRequest request, HttpHeaders headers, String id) {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT t.*\r\n")
                .append("FROM CMN_FILE_TEMPLATE t\r\n")
                .append("WHERE t.file_template_id = ?p_id\r\n");

        BaseEntity entity = AppContext.getBeanOfType(BaseEntity.class, config.getEntityName());
        List<Map<String, Object>>  templates = null;
        List<? extends BaseEntity> result    = null;

        try (Database db = new Database()) {
            templates = db.selectAsMapList(builder.toString(),
                            db.new Parameter("p_id", id));

            if (templates.size() == 0)
                return new ResponseEntity<>(
                        RestMessageEntity.getInstanceList("", String.format("No template found for the TemplateId:%s", id), RestMessageEntity.MessageStatus.FAILURE),
                        headers, HttpStatus.INTERNAL_SERVER_ERROR);

            QueryExecutor executor = new QueryExecutor(config, request, db);
            if (entity.isJpaEntity())
                result = executor.findMultiple(entity.getClass());
            else
                result = executor.fetchResultSetAsEntity(entity.getClass());
        }

        Map<String, Object> templateDb = templates.get(0);
        List<BaseEntity>    response   = new ArrayList<>();
        Template            template   = new Template(null, String.valueOf(templateDb.get(Template.NAME)), String.valueOf(templateDb.get(Template.DESCRIPTION)),
                                                        String.valueOf(templateDb.get(Template.CONTENT)),"1 == 1", "");
        for (BaseEntity current : result) {
            String mailContent = template.parse(current);
            if (!mailContent.isEmpty())
                response.add(RestMessageEntity.getInstance("", mailContent, RestMessageEntity.MessageStatus.SUCCESS));
        }
        return new ResponseEntity<>(response, headers, HttpStatus.OK);
    }
}
