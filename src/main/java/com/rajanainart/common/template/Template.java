package com.rajanainart.common.template;

import com.rajanainart.common.config.AppContext;
import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;
import com.rajanainart.common.data.QueryExecutor;
import com.rajanainart.common.rest.BaseRestController;
import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.rest.RestQueryRequest;

import java.util.*;

public class Template {
    public static final String NAME        = "NAME";
    public static final String DESCRIPTION = "DESCRIPTION";
    public static final String CONDITION   = "CONDITION";
    public static final String CONTENT     = "CONTENT";
    public static final String MAIL_TO     = "MAIL_TO";

    public static final Map<String, BaseEntity> TEMPLATE_SOURCES = AppContext.getBeansOfType(BaseEntity.class, TemplateSource.class);

    public static final String REST_QUERY_SAVE_TEMPLATE = "QrySaveTemplate";
    public static final String REST_QUERY_DEL_TEMPLATE  = "QryDeleteTemplate";

    private Optional<Long> id;
    private String   name       ;
    private String   description;
    private String   content    ;
    private String   condition  ;
    private String   emailTo    ;
    private Database db;
    private RestQueryConfig  saveConfig  ;
    private RestQueryConfig  deleteConfig;
    private RestQueryRequest queryRequest;

    public Optional<Long> getId () { return id         ; }
    public String getName       () { return name       ; }
    public String getDescription() { return description; }
    public String getContent    () { return content    ; }
    public String getCondition  () { return condition  ; }
    public String getEmailTo    () { return emailTo    ; }

    public Template(Database db, long id) {
        this.db = db;
        db.selectWithCallback("SELECT * FROM CMN_TEMPLATE WHERE template_id = ?p_id",
                (record, index) -> {
                    try {
                        name        = record.getString("name"       );
                        description = record.getString("description");
                        content     = record.getString("content"    );
                        condition   = record.getString("condition"  );
                        emailTo     = record.getString("email_to"   );
                    }
                    catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }, db.new Parameter("p_id", id));
        init();
    }

    public Template(Database db, String name, String description, String content, String condition, String emailTo) {
        this.db          = db  ;
        this.id          = null;
        this.name        = name;
        this.description = description;
        this.content     = content    ;
        this.condition   = condition  ;
        this.emailTo     = emailTo    ;
        init();
    }

    private void init() {
        saveConfig   = BaseRestController.REST_QUERY_CONFIGS.get(REST_QUERY_SAVE_TEMPLATE);
        deleteConfig = BaseRestController.REST_QUERY_CONFIGS.get(REST_QUERY_DEL_TEMPLATE);

        queryRequest = new RestQueryRequest();
        queryRequest.getParams().put("Name"       , name       );
        queryRequest.getParams().put("Description", description);
        queryRequest.getParams().put("Content"    , content    );
        queryRequest.getParams().put("Condition"  , condition  );
        queryRequest.getParams().put("EmailTo"    , emailTo    );
    }

    public void save() {
        QueryExecutor executor = new QueryExecutor(saveConfig, queryRequest, db);
        executor.executeQuery();
    }

    public void delete() {
        QueryExecutor executor = new QueryExecutor(deleteConfig, queryRequest, db);
        executor.executeQuery();
    }

    public <T extends BaseEntity> boolean validate(List<T> contextInstances) {
        Expression             expression = new Expression(condition);
        ExpressionEvaluator<T> evaluator  = new ExpressionEvaluator<>(expression, contextInstances, true);
        return evaluator.parseAsBoolean();
    }

    public <T extends BaseEntity> String parse(List<T> contextInstances) {
        Expression             expression = new Expression(content);
        ExpressionEvaluator<T> evaluator  = new ExpressionEvaluator<>(expression, contextInstances, true);
        return validate(contextInstances) ?  evaluator.parseAsString() : "";
    }
}
