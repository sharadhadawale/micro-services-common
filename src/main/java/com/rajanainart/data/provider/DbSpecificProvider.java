package com.rajanainart.data.provider;

import com.rajanainart.data.Database;
import com.rajanainart.rest.RestQueryConfig;

import java.sql.CallableStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public interface DbSpecificProvider {
    String       getParameterRegex();
    String       getParameterKey();
    String       getParameterizedQuery(String query);
    List<String> getQueryParameters(String query);

    String selectCurrentSequenceString(String sequenceName);
    List<Map<String, Object>> selectDbSpecificMapList(Database db, String query, Database.Parameter ... parameters);

    default int getCursorParameterIndex(Database.Parameter ... parameters) {
        int idx = 1;
        for (Database.Parameter p : parameters) {
            if (p.getParameterType() == RestQueryConfig.ParameterType.CURSOR) return idx;
            idx++;
        }
        return -1;
    }

    default void updateOutputParameters(CallableStatement statement, Map<String, Object> result, Database.Parameter ... parameters) throws SQLException {
        int idx = 1;
        for (Database.Parameter parameter : parameters) {
            if (parameter.isOutput() && parameter.getParameterType() != RestQueryConfig.ParameterType.CURSOR) {
                Object value = statement.getObject(idx);
                result.put(parameter.getName(), value);
            }
            idx++;
        }
    }
}
