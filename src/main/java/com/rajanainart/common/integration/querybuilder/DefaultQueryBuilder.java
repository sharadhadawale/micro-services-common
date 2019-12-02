package com.rajanainart.common.integration.querybuilder;

import com.rajanainart.common.rest.RestQueryConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DefaultQueryBuilder {
    protected RestQueryConfig config;

    public DefaultQueryBuilder(RestQueryConfig config) {
        this.config = config;
    }

    public String buildUpdateQuery(RestQueryConfig config, List<String> uniques, Map<String, Object> record) {
        StringBuilder builder = new StringBuilder();
        int index = 0;

        builder.append(String.format("UPDATE %s SET %n ", config.getTarget()));
        for (RestQueryConfig.FieldConfig f : config.getFields()) {
            if (uniques.contains(f.getId())) continue;

            builder.append(String.format("%s%s = '%s' %n ", index++ != 0 ? "," : "",
                    !f.getTargetField().isEmpty() ? f.getTargetField() : f.getId(), record.get(f.getId())));
        }
        builder.append("WHERE 1=1 \r\n");
        for (RestQueryConfig.FieldConfig f : config.getFields()) {
            if (!uniques.contains(f.getId())) continue;

            builder.append(String.format("AND %s = '%s' %n ", !f.getTargetField().isEmpty() ? f.getTargetField() : f.getId(),
                    record.get(f.getId())));
        }
        return builder.toString();
    }

    public String buildInsertQuery(RestQueryConfig config, Map<String, Object> record) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("INSERT INTO %s (", config.getTarget()));
        int index = 0;
        for (RestQueryConfig.FieldConfig f : config.getFields())
            builder.append(String.format("%s%s", index++ != 0 ? "," : "", f.getTargetField()));
        builder.append(")\r\nVALUES(\r\n");
        index = 0;
        for (RestQueryConfig.FieldConfig f : config.getFields())
            builder.append(String.format("%s'%s'", index++ != 0 ? "," : "", record.get(f.getId())));
        builder.append(")");

        return builder.toString();
    }

    public static List<String> getUniqueColumns(RestQueryConfig config) {
        List<String> cols = new ArrayList<>();
        for (RestQueryConfig.FieldConfig f : config.getFields())
            if (f.getIsPk()) cols.add(f.getTargetField());
        return cols;
    }
}
