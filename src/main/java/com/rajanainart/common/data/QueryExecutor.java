package com.rajanainart.common.data;

import com.rajanainart.common.rest.RestQueryConfig;
import com.rajanainart.common.rest.RestQueryRequest;

import java.io.Closeable;
import java.sql.ResultSet;
import java.util.List;
import java.util.Map;

public class QueryExecutor implements Closeable {
    public final String ORACLE_DATE_STRING_FORMAT = "MM/DD/YYYY";

    private Database db;
    private RestQueryConfig config;
    private RestQueryRequest request;

    public RestQueryConfig  getRestQueryConfig () { return config ; }
    public RestQueryRequest getRestQueryRequest() { return request; }
    public Database         getUnderlyingDb    () { return db     ; }

    public QueryExecutor(RestQueryConfig config, RestQueryRequest request, Database db) {
        this.config  = config;
        this.request = request;
        this.db 	 = db;
    }

    public QueryExecutor(RestQueryConfig config, RestQueryRequest request) {
        this.config  = config;
        this.request = request;
        this.db      = new Database();
    }

    public Database.Parameter[] buildDbParameters() {
        Database.Parameter[] parameters = new Database.Parameter[config.getParameters().size()];
        int index = 0;
        for (RestQueryConfig.ParameterConfig p : config.getParameters()) {
            if (request.getParams().containsKey(p.getId()))
                parameters[index++] = db.new Parameter(p.getId(), request.getParams().get(p.getId()));
            else if (request.getParamsWithObject().containsKey(p.getId()))
                parameters[index++] = db.new Parameter(p.getId(), request.getParamsWithObject().get(p.getId()));
            else
                parameters[index++] = db.new Parameter(p.getId(), "");
        }
        return parameters;
    }

    public String buildQueryWithoutParameters() {
        String query = config.getQuery();
        for (Database.Parameter p : buildDbParameters()) {
            query = query.replaceAll(String.format("%s%s", Database.QUERY_PARAMETER_REGEX, p.getName()),
                                     String.format("'%s'", p.getValue()));
        }
        return query;
    }

    public boolean isMandatoryFiltersAvailable() {
        for (BaseMessageColumn c : config.getColumns()) {
            if (!c.getIsMandatory()) continue;
            boolean found = false;
            for (QueryFilter f : request.getFilter()) {
                if (f.getFieldId().equalsIgnoreCase(c.getId())) {
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return true;
    }

    public long getTotalRecordCount() {
        long[] rows = new long[1];
        Database.Parameter[] parameters = buildDbParameters();
        db.selectWithCallback(getCountQuery(), (ResultSet rs, long index) -> {
            try {
                rows[0] = rs.getLong(1);
            }
            catch(Exception ex) {
                rows[0] = 0L;
            }
        }, parameters);
        return rows[0];
    }

    public void selectWithCallback(Database.DataResultSet callback) {
        Database.Parameter[] parameters = buildDbParameters();
        if (request.getCurrentPageNumber() != -1)
            db.selectWithCallback(getPagedQuery(), callback, parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            db.selectWithCallback(getFilteredQuery(), callback, parameters);
        db.selectWithCallback(config.getQuery(), callback, parameters);
    }

    public List<Map<String, Object>> selectAsMapList() {
        Database.Parameter[] parameters = buildDbParameters();
        if (request.getCurrentPageNumber() != -1)
            return db.selectAsMapList(getPagedQuery(), parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.selectAsMapList(getFilteredQuery(), parameters);
        return db.selectAsMapList(config.getQuery(), parameters);
    }

    public <T> List<T> fetchClassResultSetAsEntity(Class<T> clazz) {
        Database.Parameter[] parameters = buildDbParameters();
        if (!isMandatoryFiltersAvailable())
            return db.bindClassList(clazz, getEmptyQuery(), parameters);
        else if (request.getCurrentPageNumber() != -1)
            return db.bindClassList(clazz, getPagedQuery(), parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.bindClassList(clazz, getFilteredQuery(), parameters);
        return db.bindClassList(clazz, getFilteredQuery(), parameters);
    }

    public <T extends BaseEntity> List<T> fetchResultSetAsEntity(Class<T> clazz) {
        Database.Parameter[] parameters = buildDbParameters();
        if (!isMandatoryFiltersAvailable())
            return db.bindList(clazz, getEmptyQuery(), parameters);
        else if (request.getCurrentPageNumber() != -1)
            return db.bindList(clazz, getPagedQuery(), parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.bindList(clazz, getFilteredQuery(), parameters);
        return db.bindList(clazz, getFilteredQuery(), parameters);
    }

    public <T extends BaseEntity> List<T> fetchResultSetAsEntity(Class<T> clazz, Database.ValidateRecord validate) {
        Database.Parameter[] parameters = buildDbParameters();
        if (!isMandatoryFiltersAvailable())
            return db.bindList(clazz, getEmptyQuery(), validate, parameters);
        else if (request.getCurrentPageNumber() != -1)
            return db.bindList(clazz, getPagedQuery(), validate, parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.bindList(clazz, getFilteredQuery(), validate, parameters);
        return db.bindList(clazz, config.getQuery(), validate, parameters);
    }

    public <T extends BaseEntity> List<T> findMultiple(Class<T> clazz) {
        Database.Parameter[] parameters = buildDbParameters();
        if (!isMandatoryFiltersAvailable())
            return db.findMultiple(clazz, getEmptyQuery(), parameters);
        else if (request.getCurrentPageNumber() != -1)
            return db.findMultiple(clazz, getPagedQuery(), parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.findMultiple(clazz, getFilteredQuery(), parameters);
        return db.findMultiple(clazz, config.getQuery(), parameters);
    }

    public int executeQuery() {
        Database.Parameter[] parameters = buildDbParameters();
        return db.executeQueryWithJdbc(config.getActualQuery(), parameters);
    }

    public List<Map<String, Object>> fetchResultSet() {
        Database.Parameter[] parameters = buildDbParameters();
        if (!isMandatoryFiltersAvailable())
            return db.selectAsMapList(getEmptyQuery(), parameters);
        else if (request.getCurrentPageNumber() != -1)
            return db.selectAsMapList(getPagedQuery(), parameters);
        else if (request.hasCurrentPageNumber() && request.getCurrentPageNumber() == -1)
            return db.selectAsMapList(getFilteredQuery(), parameters);
        return db.selectAsMapList(getFilteredQuery(), parameters);
    }

    public String getCountQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT COUNT(*)\r\n")
                .append("FROM(\r\n")
                .append(config.getQuery()).append("\r\n")
                .append(")tbl$\r\n")
                .append("WHERE \r\n")
                .append("1 = 1")
                .append(getConditionExpression());

        return builder.toString();
    }

    public String getEmptyQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ROW_NUMBER() OVER () AS ROWNUM$, tbl$.*\r\n")
                .append("FROM(\r\n")
                .append(config.getQuery()).append("\r\n")
                .append(") tbl$\r\n")
                .append("WHERE\r\n")
                .append("1 != 1");
        return builder.toString();
    }

    public String getFilteredQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT ROW_NUMBER() OVER () AS ROWNUM$, tbl$.*\r\n")
                .append("FROM (\r\n")
                .append(config.getQuery()).append("\r\n")
                .append(") tbl$\r\n")
                .append("WHERE\r\n")
                .append("1 = 1")
                .append(getConditionExpression());

        return builder.toString();
    }

    public String getPagedQuery() {
        StringBuilder builder = new StringBuilder();
        builder.append("SELECT tbl$$.*\r\n")
                .append("FROM (\r\n")
                .append(getFilteredQuery()).append("\r\n")
                .append(") tbl$$\r\n")
                .append("WHERE ")
                .append(String.format("ROWNUM$ >= ((%s*(%s-1))+1) ", request.getPageSize().orElse(config.getPageSize()), request.getCurrentPageNumber()))
                .append(String.format("AND ROWNUM$ <= %s*%s", request.getPageSize().orElse(config.getPageSize()), request.getCurrentPageNumber()));
        return builder.toString();
    }

    public String getConditionExpression() {
        StringBuilder builder = new StringBuilder();
        for (QueryFilter filter : request.getFilter()) {
            builder.append(" AND ");
            switch (filter.getActualOperator()) {
                case LIKE:
                case NOT_LIKE:
                    builder.append(String.format("%s %s '%s'", filter.getFieldId(), filter.getOperator(), "%"+filter.getValue1()+"%"));
                    break;
                case NULL:
                case NOT_NULL:
                    builder.append(String.format("%s IS %s", filter.getFieldId(), filter.getOperator()));
                    break;
                case IN:
                case NOT_IN:
                    builder.append(String.format("%s %s (%s)", filter.getFieldId(), filter.getOperator(),
                            convertToQueryFilterString(filter.getValue1(), ",")));
                    break;
                case GREATER_THAN:
                case GREATER_THAN_EQUAL_TO:
                case LESSER_THAN:
                case LESSER_THAN_EQUAL_TO:
                    if (filter.getActualFieldType() == BaseMessageColumn.ColumnType.DATE)
                        builder.append(String.format("TO_CHAR(%s, '%s') %s '%s'", filter.getFieldId(), ORACLE_DATE_STRING_FORMAT, filter.getOperator(), filter.getValue1()));
                    else
                        builder.append(String.format("%s %s %s", filter.getFieldId(), filter.getOperator(), filter.getValue1()));
                    break;
                case BETWEEN:
                case NOT_BETWEEN:
                    if (filter.getActualFieldType() == BaseMessageColumn.ColumnType.INTEGER || filter.getActualFieldType() == BaseMessageColumn.ColumnType.NUMERIC)
                        builder.append(String.format("%s %s %s AND %s", filter.getFieldId(), filter.getOperator(), filter.getValue1(), filter.getValue2()));
                    else if (filter.getActualFieldType() == BaseMessageColumn.ColumnType.DATE)
                        builder.append(String.format("TO_CHAR(%s, '%s') %s '%s' AND '%s'", filter.getFieldId(), ORACLE_DATE_STRING_FORMAT, filter.getOperator(), filter.getValue1(), filter.getValue2()));
                    break;
                case EQUAL:
                case NOT_EQUAL:
                    if (filter.getActualFieldType() == BaseMessageColumn.ColumnType.INTEGER || filter.getActualFieldType() == BaseMessageColumn.ColumnType.NUMERIC)
                        builder.append(String.format("%s %s %s", filter.getFieldId(), filter.getOperator(), filter.getValue1()));
                    else if (filter.getActualFieldType() == BaseMessageColumn.ColumnType.DATE)
                        builder.append(String.format("TO_CHAR(%s, '%s') %s '%s'", filter.getFieldId(), ORACLE_DATE_STRING_FORMAT, filter.getOperator(), filter.getValue1()));
                    else
                        builder.append(String.format("%s %s '%s'", filter.getFieldId(), filter.getOperator(), filter.getValue1()));
                    break;
                default:
                    break;
            }
        }
        return builder.toString();
    }

    public static String convertToQueryFilterString(String value, String seperator) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (String v : value.split(seperator)) {
            builder.append(String.format("%s'%s'", !isFirst?",":"", v));
            isFirst = false;
        }
        return builder.toString();
    }

    @Override
    public void close() {
        if (db != null)
            db.close();
    }
}
