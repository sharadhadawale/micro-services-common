package com.rajanainart.common.integration.querybuilder;

import com.rajanainart.common.data.BaseMessageColumn;
import com.rajanainart.common.data.BaseMessageTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("query-builder-org.hibernate.dialect.PostgreSQLDialect")
public class PostgresDbSpecificQueryBuilder implements DbSpecficQueryBuilder {
    @Override
    public String delete(BaseMessageTable config, Map<String, String> requestParams) {
        int index = 0;
        int count = 0;
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("DELETE FROM %s %n ", config.getTarget()))
                .append("WHERE ");
        for (BaseMessageColumn f : config.getColumns()) {
            if (!f.getIsPk()) continue;
            if (!requestParams.containsKey(f.getTargetField())) continue;

            switch (f.getType()) {
                case NUMERIC:
                case INTEGER:
                    builder.append(String.format("%s%s = %s", index++ != 0 ? " AND  " : "", f.getTargetField(),
                                        requestParams.get(f.getTargetField())));
                    break;
                case TEXT:
                    builder.append(String.format("%s%s = '%s'", index++ != 0 ? " AND " : "", f.getTargetField(),
                                        requestParams.get(f.getTargetField()).replace("'", "''")));
                    break;
                case DATE:
                    builder.append(String.format("%s%s = TO_DATE('%s', '%s')", index++ != 0 ? " AND " : "", f.getTargetField(),
                                        requestParams.get(f.getTargetField()), f.getFormat()));
                    break;
            }
            count++;
        }
        if (count == 0) throw new IllegalArgumentException("No conditions found for deleting the rows");
        return builder.toString();
    }

    @Override
    public String bulkUpdate(BaseMessageTable config, List<Map<String, Object>> records) {
        StringBuilder builder = new StringBuilder();

        int index  = 0;
        int index1 = 0;

        builder.append(String.format("INSERT INTO %s( %n ", config.getTarget()));
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr()) continue;
            builder.append(String.format("%s%s", index++ != 0 ? "," : "", f.getTargetField()));
        }
        builder.append(")\r\n");

        index1 = 0;
        builder.append("WITH source AS (\r\n");
        for (Map<String, Object> record : records) {
            index = 0;
            builder.append(String.format("%s %n ", index1++ != 0 ? "UNION ALL" : ""))
                    .append("SELECT ");
            for (BaseMessageColumn f : config.getColumns()) {
                if (f.getAutoIncr()) continue;
                switch (f.getType()) {
                    case INTEGER:
                    case NUMERIC:
                        builder.append(String.format("%s%s AS %s", index++ != 0 ? "," : "", record.get(f.getId()), f.getId()));
                        break;
                    case TEXT:
                        builder.append(String.format("%s'%s' AS %s", index++ != 0 ? "," : "", record.get(f.getId()), f.getId()));
                        break;
                    case DATE:
                        builder.append(String.format("%s TO_DATE('%s', '%s') AS %s",
                                                    index++ != 0 ? "," : "",
                                                    record.get(f.getId()) != null ? record.get(f.getId()) : "",
                                                    f.getFormat(), f.getId()));
                        break;
                }
            }
            builder.append("\r\n");
        }
        builder.append(")\r\n")
                .append("SELECT * FROM source s\r\n");
        return builder.toString();
    }

    @Override
    public String rowUpdate(BaseMessageTable config, Map<String, Object> record) {
        return "";
    }
}
