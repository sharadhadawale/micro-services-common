package com.rajanainart.integration.querybuilder;

import com.rajanainart.data.BaseMessageColumn;
import com.rajanainart.data.BaseMessageTable;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component("query-builder-org.hibernate.dialect.Oracle10gDialect")
public class OracleDbSpecificQueryBuilder implements DbSpecficQueryBuilder {
    @Override
    public String delete(BaseMessageTable config, Map<String, String> requestParams) {
        return "";
    }

    @Override
    public String bulkUpdate(BaseMessageTable config, List<Map<String, Object>> records) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("MERGE INTO %s t %n ", config.getTarget()));

        int index = 0;
        builder.append("USING (\r\n")
               .append("WITH source AS (\r\n");
        int index1 = 0;
        for (Map<String, Object> record : records) {
            index = 0;
            builder.append(String.format("%s %n ", index1++ != 0 ? "UNION ALL" : ""))
                   .append("SELECT ");
            for (BaseMessageColumn f : config.getColumns()) {
                if (f.getAutoIncr() || f.isDynamicField()) continue;

                if (f.getType() == BaseMessageColumn.ColumnType.TEXT)
                    builder.append(String.format("%s'%s' AS %s", index++ != 0 ? "," : "", String.valueOf(record.get(f.getId())).replace("'", "''"), f.getId()));
                else
                    builder.append(String.format("%s'%s' AS %s", index++ != 0 ? "," : "", record.get(f.getId()), f.getId()));
            }
            builder.append("FROM DUAL\r\n");
        }
        builder.append(") \r\n")
               .append("SELECT * FROM source\r\n")
               .append(") s\r\n")
               .append("ON (");

        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (!f.getIsPk() || f.isDynamicField()) continue;

            builder.append(String.format("%s s.%s = t.%s", index++ != 0 ? "AND" : "", f.getId(), f.getTargetField()));
        }
        builder.append(") \r\n")
               .append("WHEN NOT MATCHED INSERT (");

        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr() || f.isDynamicField()) continue;
            builder.append(String.format("%s%s", index++ != 0 ? "," : "", f.getTargetField()));
        }
        builder.append(")\r\n")
               .append("VALUES (");

        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr() || f.isDynamicField()) continue;
            builder.append(String.format("%s s.%s", index++ != 0 ? "," : "", f.getId()));
        }
        builder.append(")\r\n")
               .append("WHEN MATCHED UPDATE SET\r\n");

        index = 0;
        for (BaseMessageColumn f : config.getColumns()) {
            if (f.getAutoIncr() || f.isDynamicField()) continue;
            builder.append(String.format("%s t.%s = s.%s %n ", index++ != 0 ? "," : "", f.getTargetField(), f.getId()));
        }
        return builder.toString();
    }

    @Override
    public String rowUpdate(BaseMessageTable config, Map<String, Object> record) {
        return "";
    }
}
