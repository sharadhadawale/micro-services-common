package com.rajanainart.rest;

import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.*;

import com.rajanainart.cache.CacheException;
import com.rajanainart.cache.CacheItem;
import com.rajanainart.cache.CacheManager;
import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.helper.ReflectionHelper;
import com.rajanainart.rest.validator.DataTypeValidator;
import com.rajanainart.rest.validator.LogicalValidator;
import com.rajanainart.rest.validator.MandatoryValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;
import com.rajanainart.data.QueryExecutor;

import javax.persistence.PersistenceException;

@Component("restentityhandler-default")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultEntityHandler implements BaseEntityHandler {
    private static final Logger logger = LogManager.getLogger(DefaultEntityHandler.class);

    private RestQueryConfig  config  ;
    private RestQueryRequest request ;
    private QueryExecutor    executor;
    private Database         db      ;

    public RestQueryConfig  getRestQueryConfig () { return config  ; }
    public RestQueryRequest getRestQueryRequest() { return request ; }
    public QueryExecutor	getQueryExecutor   () { return executor; }
    public Database	        getUnderlyingDb	   () { return db 	   ; }

    public <T extends BaseEntity> void setup(Class<T> clazz, RestQueryConfig config, RestQueryRequest request, Database db) {
        this.config   = config;
        this.request  = request;
        this.db       = db;
        this.executor = new QueryExecutor(config, request, db);
    }

    private String validate(Map<String, String> params, RestQueryConfig.ValidationExecutionType type) {
        String success = BaseRestController.REST_DATA_VALIDATORS.get(MandatoryValidator.VALIDATOR_KEY).validate(config, type, params, request.getParamsWithObject());
        if (success.equals(BaseRestController.SUCCESS))
            success = BaseRestController.REST_DATA_VALIDATORS.get(DataTypeValidator.VALIDATOR_KEY).validate(config, type, params, request.getParamsWithObject());
        if (success.equals(BaseRestController.SUCCESS))
            success = BaseRestController.REST_DATA_VALIDATORS.get(LogicalValidator.VALIDATOR_KEY).validate(config, type, params, request.getParamsWithObject());
        return success;
    }

    public String preValidateRestEntity() {
        return validate(request.getParams(), RestQueryConfig.ValidationExecutionType.PRE_DATA_FETCH);
    }

    public String preValidateRestEntity(RestQueryConfig config, RestQueryRequest request) {
        this.config  = config;
        this.request = request;
        return validate(request.getParams(), RestQueryConfig.ValidationExecutionType.PRE_DATA_FETCH);
    }

    public <T extends BaseEntity> String validateRecordRestEntity(Class<T> entityClass, ResultSet record) {
        Map<String, String> params = BaseRestController.REST_DATA_VALIDATORS.get(MandatoryValidator.VALIDATOR_KEY).buildResultSetReturnValues(entityClass, String.class, record);
        return validate(params, RestQueryConfig.ValidationExecutionType.WHILE_DATA_FETCH);
    }

    public <T extends BaseEntity> String postValidateRestEntity(List<T> input) {
        String success = BaseRestController.SUCCESS;
        for (T instance : input) {
            Map<String, String> params = BaseRestController.REST_DATA_VALIDATORS.get(MandatoryValidator.VALIDATOR_KEY).buildMethodReturnValues(String.class, instance);
            success = validate(params, RestQueryConfig.ValidationExecutionType.POST_DATA_FETCH);

            if (!success.equals(BaseRestController.SUCCESS)) break;
        }
        return success;
    }

    public List<BaseEntity> executeQuery(StringBuilder message) {
        List<BaseEntity> result = null;
        try {
            int r = executor.executeQuery();
            message.append(BaseRestController.SUCCESS);
            result = RestMessageEntity.getInstanceList("", String.format("Record has %sbeen updated", r > 0 ? "" : "not "),
                                                        RestMessageEntity.MessageStatus.SUCCESS);
        }
        catch(PersistenceException ex1) {
            if (ex1.getCause().getClass() == SQLIntegrityConstraintViolationException.class) {
                message.append(ex1.getMessage());
                result = RestMessageEntity.getInstanceList("", "Record already exists", RestMessageEntity.MessageStatus.FAILURE);
            }
        }
        return result;
    }

    public <T extends BaseEntity> List<T> fetchRestQueryResultSet(Class<T> clazz, StringBuilder message) {
        List<T> list    = null;
        String  jpaName = ReflectionHelper.getJpaTableName(clazz);

        if (!jpaName.isEmpty())
            list = executor.findMultiple(clazz);
        else
            list = executor.fetchResultSetAsEntity(clazz,
                            (ResultSet record, long currentRowIndex) -> {
                                String msg = validateRecordRestEntity(clazz, record);
                                if (!BaseRestController.SUCCESS.equalsIgnoreCase(msg)) {
                                    message.append(msg);
                                    return false;
                                }
                                return true;
                            }
                    );
        return list;
    }

    public List<Map<String, Object>> fetchRestQueryAsMap() {
        List<Map<String, Object>> parent = new ArrayList<>();
        parent.addAll(config.hasCursorParameter() ? executor.selectDbSpecificMapList() : executor.selectAsMapList());

        for (RestQueryConfig.FieldConfig field : config.getFields()) {
            if (field.getType() == BaseMessageColumn.ColumnType.SUBLIST) {
                RestQueryConfig subConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(field.getSelectQuery(), null);
                if (subConfig != null) {
                    QueryExecutor subExecutor = new QueryExecutor(subConfig, request, db);
                    List<Map<String, Object>> child = subConfig.hasCursorParameter() ? subExecutor.selectDbSpecificMapList():
                                                                                       subExecutor.selectAsMapList();
                    bindResultSet(parent, child, field);
                }
            } else if (field.getType() == BaseMessageColumn.ColumnType.SELECT || field.getType() == BaseMessageColumn.ColumnType.SINGLE_SELECT) {
                if (parent.size() == 0) parent.add(new HashMap<>());

                RestQueryConfig subConfig = BaseRestController.REST_QUERY_CONFIGS.getOrDefault(field.getSelectQuery(), null);
                if (subConfig != null) {
                    logger.info(String.format("Building SELECT result-set for:%s-%s", config.getId(), subConfig.getId()));

                    CacheManager manager  = null;
                    List<Map<String, Object>> cachedResult = null;
                    if (subConfig.isCacheEnabled()) {
                        try {
                            manager      = new CacheManager(subConfig, request.buildRequestForConfig(subConfig));
                            cachedResult = manager.getCachedMapRecords();
                            if (cachedResult != null) {
                                logger.info(String.format("Resolved from Cache server:%s", subConfig.getId()));
                                String key = String.format("%s_array", field.getId());
                                parent.get(0).put(key, cachedResult);
                            }
                        }
                        catch (CacheException ex) {
                            ex.printStackTrace();
                        }
                    }

                    if (cachedResult == null) {
                        QueryExecutor subExecutor = new QueryExecutor(subConfig, request, db);
                        List<Map<String, Object>> child = subConfig.hasCursorParameter() ? subExecutor.selectDbSpecificMapList():
                                                                                           subExecutor.selectAsMapList();
                        String key = String.format("%s_array", field.getId());
                        parent.get(0).put(key, child);

                        if (subConfig.isCacheEnabled() && manager != null) {
                            CacheItem item = manager.saveMapRecords(child);
                            logger.info(String.format("Stored to cache server:%s:%s", subConfig.getId(), item.buildCacheKey()));
                        }
                    }
                }
            }
        }
        return parent;
    }

    public void bindResultSet(List<Map<String, Object>> parent, List<Map<String, Object>> child, RestQueryConfig.FieldConfig link) {
        String key = String.format("%s_array", link.getId());
        for (Map<String, Object> pr : parent) {
            if (pr.getOrDefault(key, null) == null)
                pr.put(key, new ArrayList<>());
            for (Map<String, Object> cr : child) {
                if (pr.containsKey(link.getId()) && cr.containsKey(link.getId())) {
                    String pid = String.valueOf(pr.get(link.getId()));
                    String cid = String.valueOf(cr.get(link.getId()));
                    if (pid.equalsIgnoreCase(cid))
                        ((List) pr.get(key)).add(cr);
                }
            }
        }
    }

    public  <T extends BaseEntity> List<T> executePostAction(Class<T> clazz, List<T>   input) { return input; }
    public List<Map<String, Object>>       executePostAction(List<Map<String, Object>> input) { return input; }

    public boolean validateCacheExpiry(CacheItem item) {
        return true;
    }
}
