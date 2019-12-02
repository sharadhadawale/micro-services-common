package com.rajanainart.common.rest;

import java.sql.ResultSet;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.List;
import java.util.Map;

import com.rajanainart.common.helper.ReflectionHelper;
import com.rajanainart.common.rest.validator.DataTypeValidator;
import com.rajanainart.common.rest.validator.LogicalValidator;
import com.rajanainart.common.rest.validator.MandatoryValidator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.data.QueryExecutor;

import javax.persistence.PersistenceException;

@Component("restentityhandler-default")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class DefaultEntityHandler implements BaseEntityHandler {

    private RestQueryConfig  config  ;
    private RestQueryRequest request ;
    private QueryExecutor    executor;
    private Database         db      ;

    public RestQueryConfig  getRestQueryConfig () { return config  ; }
    public RestQueryRequest getRestQueryRequest() { return request ; }
    protected QueryExecutor	getQueryExecutor   () { return executor; }
    protected Database	    getUnderlyingDb	   () { return db 	   ; }

    public <T extends BaseEntity> void setup(Class<T> clazz, RestQueryConfig config, RestQueryRequest request, Database db) {
        this.config   = config;
        this.request  = request;
        this.db       = db;
        this.executor = new QueryExecutor(config, request, db);
    }

    private String validate(Map<String, String> params, RestQueryConfig.ValidationExecutionType type) {
        String success = BaseRestController.REST_DATA_VALIDATORS.get(MandatoryValidator.VALIDATOR_KEY).validate(config, type, params);
        if (success.equals(BaseRestController.SUCCESS))
            success = BaseRestController.REST_DATA_VALIDATORS.get(DataTypeValidator.VALIDATOR_KEY).validate(config, type, params);
        if (success.equals(BaseRestController.SUCCESS))
            success = BaseRestController.REST_DATA_VALIDATORS.get(LogicalValidator.VALIDATOR_KEY).validate(config, type, params);
        return success;
    }

    public String preValidateRestEntity() {
        return validate(request.getParams(), RestQueryConfig.ValidationExecutionType.PRE_DATA_FETCH);
    }

    public String preValidateRestEntity(RestQueryConfig config, RestQueryRequest request) {
        this.config = config;
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
            if (r > 0) {
                message.append(BaseRestController.SUCCESS);
                result = RestMessageEntity.getInstanceList("", "Record has been updated", RestMessageEntity.MessageStatus.SUCCESS);
            }
            else {
                message.append(BaseRestController.SUCCESS);
                result = RestMessageEntity.getInstanceList("", "Record has not been updated", RestMessageEntity.MessageStatus.SUCCESS);
            }
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
        return executePostAction(clazz, list);
    }

    protected <T extends BaseEntity> List<T> executePostAction(Class<T> clazz, List<T> input) {
        return input;
    }
}
