package com.rajanainart.common.rest;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.helper.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import org.springframework.web.util.HtmlUtils;

@RestController
@RequestMapping("/jpa-rest")
public class JpaRestController extends BaseRestController {
    private static final Logger logger = LogManager.getLogger(JpaRestController.class);
    private String msg = "No entity found with the name %s";

    @RequestMapping(value = "/get/{entity:[a-zA-Z0-9]*}", method = RequestMethod.GET)
    @ResponseBody
    public <T> ResponseEntity<T> get(@PathVariable("entity") String entityName, @RequestParam("id") int id) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?>    clazz   = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        T instance;
        try (Database db = new Database()) {
            instance = (T)db.find(clazz, id);
        }
        return new ResponseEntity<>(instance, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/get-all/{entity:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<List> getAll(@PathVariable("entity") String entityName, @RequestBody Map<String, Object> conditions) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?>    clazz   = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(null, headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        List   list;
        String tableName = ReflectionHelper.getJpaTableName(clazz);
        String query     = String.format("SELECT * FROM %s WHERE %s", tableName, ReflectionHelper.getJpaCondition(clazz, conditions));

        try (Database db = new Database()) {
            list = db.findMultiple(clazz, query);
        }
        return new ResponseEntity<>(list, headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/delete/{entity:[a-zA-Z0-9]*}", method = RequestMethod.GET)
    @ResponseBody
    public ResponseEntity<BaseEntity> delete(@PathVariable("entity") String entityName, @RequestParam("id") int id) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?>    clazz   = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(RestMessageEntity.getInstance("",m, RestMessageEntity.MessageStatus.FAILURE),
                            headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try (Database db = new Database()) {
            db.deleteById(clazz, id);
            db.commit();
        }
        catch(Exception ex) {
        	return new ResponseEntity<>(RestMessageEntity.getInstance("","Error while deleting the record",RestMessageEntity.MessageStatus.FAILURE),headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(RestMessageEntity.getInstance("","Record deleted successfully",RestMessageEntity.MessageStatus.SUCCESS),headers, HttpStatus.OK);

    }

    @RequestMapping(value = "/save/{entity:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseEntity> save(@PathVariable("entity") String entityName, @RequestBody Map<String, Object> record) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?> clazz = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(
                    RestMessageEntity.getInstance("", m, RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Object instance = ReflectionHelper.getInstanceFromMap(clazz, record);
        if (instance != null) {
            try (Database db = new Database()) {
                db.save(instance);
                db.commit();
            } catch (Exception ex) {
                if (ex.getCause().getCause() != null && ex.getCause().getCause().getCause() != null && ex.getCause().getCause().getCause().getClass() == SQLIntegrityConstraintViolationException.class) {
                    return new ResponseEntity<>(RestMessageEntity.getInstance("", "Record already exists", RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
                }
                return new ResponseEntity<>(RestMessageEntity.getInstance("", ex.getMessage(), RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            return new ResponseEntity<>(RestMessageEntity.getInstance("", "Record saved Successfully",RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(RestMessageEntity.getInstance("","Could not build hibernate entity from the parameters", RestMessageEntity.MessageStatus.FAILURE),headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/save-all/{entity:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseEntity> saveAll(@PathVariable("entity") String entityName, @RequestBody List<Map<String, Object>> records) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?> clazz = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(
                    RestMessageEntity.getInstance("", m, RestMessageEntity.MessageStatus.FAILURE),
                    headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try (Database db = new Database()) {
            for (Map<String, Object> record : records) {
                Object instance = ReflectionHelper.getInstanceFromMap(clazz, record);
                if (instance != null)
                    db.save(instance);
                else
                    return new ResponseEntity<>(RestMessageEntity.getInstance("","Could not build hibernate entity from the parameters", RestMessageEntity.MessageStatus.FAILURE),headers, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            db.commit();
        } catch (Exception ex) {
            if (ex.getCause().getCause() != null && ex.getCause().getCause().getCause() != null && ex.getCause().getCause().getCause().getClass() == SQLIntegrityConstraintViolationException.class) {
                return new ResponseEntity<>(RestMessageEntity.getInstance("", "Record already exists", RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
            }
            return new ResponseEntity<>(RestMessageEntity.getInstance("", ex.getMessage(), RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        return new ResponseEntity<>(RestMessageEntity.getInstance("", "Records have been saved Successfully",RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
    }

    @RequestMapping(value = "/update/{entity:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseEntity> update(@PathVariable("entity") String entityName, @RequestBody Map<String, Object> record) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?>    clazz   = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(RestMessageEntity.getInstance("", m, RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        Object instance = ReflectionHelper.getInstanceFromMap(clazz, record);
        if (instance != null) {
            try (Database db = new Database()) {
                db.update(instance);
                db.commit();
            } catch (Exception ex) {
                return new ResponseEntity<>(RestMessageEntity.getInstance("", ex.getMessage(), RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
            }
            return new ResponseEntity<>(RestMessageEntity.getInstance("", "Record updated Successfully",RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(RestMessageEntity.getInstance("","Could not build hibernate entity from the parameters", RestMessageEntity.MessageStatus.FAILURE),headers, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @RequestMapping(value = "/update-all/{entity:[a-zA-Z0-9]*}", method = RequestMethod.POST)
    @ResponseBody
    public ResponseEntity<BaseEntity> updateAll(@PathVariable("entity") String entityName, @RequestBody List<Map<String, Object>> records) {
        String      escaped = HtmlUtils.htmlEscape(entityName);
        HttpHeaders headers = buildHttpHeaders(RestQueryConfig.RestQueryContentType.JSON.toString());
        Class<?>    clazz   = AppContext.getClassTypeOf(escaped);
        if (clazz == null) {
            String m = String.format(msg, escaped);
            logger.info(m);
            return new ResponseEntity<>(RestMessageEntity.getInstance("", m, RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try (Database db = new Database()) {
            for (Map<String, Object> record : records) {
                Object instance = ReflectionHelper.getInstanceFromMap(clazz, record);
                if (instance != null)
                    db.update(instance);
                else
                    return new ResponseEntity<>(RestMessageEntity.getInstance("", "Could not build hibernate entity from the parameters", RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.INTERNAL_SERVER_ERROR);
            }
            db.commit();
        }
        catch (Exception ex) {
            return new ResponseEntity<>(RestMessageEntity.getInstance("", ex.getMessage(), RestMessageEntity.MessageStatus.FAILURE), headers, HttpStatus.OK);
        }
        return new ResponseEntity<>(RestMessageEntity.getInstance("", "Records have been updated Successfully", RestMessageEntity.MessageStatus.SUCCESS), headers, HttpStatus.OK);
    }
}
