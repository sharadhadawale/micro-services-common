package com.rajanainart.data.provider;

import com.rajanainart.data.Database;
import com.rajanainart.rest.RestQueryConfig;
import oracle.jdbc.OracleConnection;
import oracle.jdbc.OracleTypes;
import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import org.hibernate.type.*;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("deprecation")
@Component("org.hibernate.dialect.Oracle10gDialect")
public class OracleDbProvider implements DbSpecificProvider {
    public String getParameterRegex() {
        return "[\\?][a-zA-Z0-9\\_\\.]*";
    }

    public String getParameterKey() {
        return "\\?";
    }

    public String getParameterizedQuery(String query) {
        Pattern pattern = Pattern.compile(getParameterRegex());
        Matcher matcher = pattern.matcher(query);
        String  updated = query;
        while (matcher.find())
            updated = matcher.replaceAll(getParameterKey());
        return updated;
    }

    public List<String> getQueryParameters(String query) {
        List<String> params = new ArrayList<>();
        Pattern pattern = Pattern.compile(getParameterRegex());
        Matcher matcher = pattern.matcher(query);
        while (matcher.find())
            params.add(matcher.group().replaceFirst(getParameterKey(), ""));
        return params;
    }

    public String selectCurrentSequenceString(String sequenceName) {
        return String.format("SELECT %s.CURRVAL FROM dual", sequenceName);
    }

    public List<Map<String, Object>> selectDbSpecificMapList(Database db, String query, Database.Parameter ... parameters) {
        List<Map<String, Object>> result = new ArrayList<>();
        db.getSession().doWork(
                (Connection connection) -> {
                    OracleConnection oracleConnection = connection.unwrap(OracleConnection.class);
                    String updated = getParameterizedQuery(query);
                    CallableStatement statement = oracleConnection.prepareCall(updated);
                    bindNamedParameters(db, statement, query, parameters);
                    statement.execute();

                    result.addAll(parseCursorResult(statement, parameters));
                    if (result.size() == 0) result.add(new HashMap<>());
                        updateOutputParameters(statement, result.get(0), parameters);

                    statement.close();
                }
        );
        return result;
    }

    public List<Map<String, Object>> parseCursorResult(CallableStatement statement, Database.Parameter ... parameters) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        int idx = getCursorParameterIndex(parameters);
        if (idx != -1) {
            ResultSet rs = (ResultSet)statement.getObject(idx);
            while (rs.next()) {
                Map<String, Object> r = Database.buildResultSetAsMap(rs);
                result.add(r);
            }
            rs.close();
        }
        return result;
    }

    public void bindNamedParameters(Database db, CallableStatement statement, String query, Database.Parameter[] parameters) throws SQLException {
        List<String> queryParams = getQueryParameters(query);
        int idx = 1;
        for (String p : queryParams) {
            Database.Parameter parameter = db.getParameter(parameters, p);
            if (parameter != null) {
                if (parameter.isOutput())
                    bindOutputParameter(statement, parameter, idx);
                else
                    bindInputParameter(statement, parameter, idx);
                idx++;
            }
        }
    }

    public void bindInputParameter(CallableStatement statement, Database.Parameter parameter, int idx) throws SQLException {
        if (parameter.getParameterType() == RestQueryConfig.ParameterType.SCALAR)
            Database.bindParameter(statement, parameter, idx);
        else if (parameter.getParameterType() == RestQueryConfig.ParameterType.STRING_ARRAY ||
                 parameter.getParameterType() == RestQueryConfig.ParameterType.NUMERIC_ARRAY) {
            ArrayDescriptor descriptor = ArrayDescriptor.createDescriptor(parameter.getNameAdditional(), statement.getConnection());
            ARRAY           array      = new ARRAY(descriptor, statement.getConnection(), ((List)parameter.getValue()).toArray());
            statement.setArray(idx, array);
        }
        else if (parameter.getParameterType() == RestQueryConfig.ParameterType.RECORD_OBJECT) {
        }
    }

    public void bindOutputParameter(CallableStatement statement, Database.Parameter parameter, int idx) throws SQLException {
        if (parameter.getParameterType() == RestQueryConfig.ParameterType.CURSOR)
            statement.registerOutParameter(idx, OracleTypes.CURSOR);
        else if (parameter.getType() == LongType.INSTANCE || parameter.getType() == DoubleType.INSTANCE)
            statement.registerOutParameter(idx, OracleTypes.NUMERIC);
        else if (parameter.getType() == StringType.INSTANCE)
            statement.registerOutParameter(idx, OracleTypes.VARCHAR);
    }
}
