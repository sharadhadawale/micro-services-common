package com.rajanainart.common.integration.querybuilder;

import com.rajanainart.common.data.BaseMessageColumn;
import com.rajanainart.common.data.BaseMessageTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("query-builder-org.hibernate.dialect.MySQLDialect")
public class MemsqlDbSpecificQueryBuilder implements DbSpecficQueryBuilder {
    @Override
    public String delete(BaseMessageTable config, Map<String, String> requestParams) {
        return "";
    }

    @Override
    public String bulkUpdate(BaseMessageTable config, List<Map<String, Object>> records) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("INSERT INTO %s (", config.getTarget()));
        int index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr()) continue;
            builder.append(String.format("%s%s", index++ != 0 ? "," : "", f.getTargetField()));
        }
        builder.append(") \r\n")
               .append("WITH source AS (\r\n");

        int index1 = 0;
        for (Map<String, Object> record : records) {
            index = 0;
            builder.append(String.format("%s %n ", index1++ != 0 ? "UNION ALL" : ""))
                   .append("SELECT ");
            for (BaseMessageColumn f : config.getColumns()) {
                if (f.getAutoIncr()) continue;

                if (f.getType() == BaseMessageColumn.ColumnType.TEXT)
                    builder.append(String.format("%s'%s' AS %s", index++ != 0 ? "," : "", String.valueOf(record.get(f.getId())).replace("'", "''"), f.getId()));
                else
                    builder.append(String.format("%s'%s' AS %s", index++ != 0 ? "," : "", record.get(f.getId()), f.getId()));
            }
            builder.append("\r\n");
        }
        builder.append(")\r\n")
               .append("SELECT * FROM source\r\n")
               .append("ON DUPLICATE KEY UPDATE\r\n");
        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr() || f.getIsPk()) continue;
            builder.append(String.format("%s%s = VALUES(%s) %n ", index++ != 0 ? "," : "", f.getTargetField(), f.getTargetField()));
        }
        return builder.toString();
    }

    @Override
    public String rowUpdate(BaseMessageTable config, Map<String, Object> record) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("INSERT INTO %s (", config.getTarget()));
        int index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr()) continue;
            builder.append(String.format("%s%s", index++ != 0 ? "," : "", f.getTargetField()));
        }
        builder.append(")\r\nVALUES(\r\n");
        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr()) continue;
            builder.append(String.format("%s'%s'", index++ != 0 ? "," : "", record.get(f.getId())));
        }
        builder.append(")\r\n");

        index = 0;
        builder.append("ON DUPLICATE KEY UPDATE\r\n");
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr() || f.getIsPk()) continue;
            builder.append(String.format("%s%s = '%s' %n ", index++ != 0 ? "," : "",
                    !f.getTargetField().isEmpty() ? f.getTargetField() : f.getId(), record.get(f.getId())));
        }
        return builder.toString();
    }
}
