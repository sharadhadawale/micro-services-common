package com.rajanainart.transform;

import com.rajanainart.data.Database;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("transform-foreign_key")
public class ForeignKeyTransform implements BaseTransform {
    private static final Logger logger = LogManager.getLogger(ForeignKeyTransform.class);

    public Object transform(Database db, Object input, Map<String, String> params) {
        String tableKey  = "param-parent-table";
        String columnKey = "param-parent-id-column";
        String uniqueKey = "param-parent-unique-column";
        if (params == null || !params.containsKey(tableKey) ||
            !params.containsKey(columnKey) || !params.containsKey(uniqueKey))
            throw new IndexOutOfBoundsException("Foreign Key transform requires 3 parameters from the config, param-parent-table, param-parent-id-column, param-parent-unique-column");
        if (params.get(tableKey ).isEmpty() ||
            params.get(columnKey).isEmpty() || params.get(uniqueKey).isEmpty())
            throw new IndexOutOfBoundsException("Foreign Key transform requires 3 parameters from the config, param-parent-table, param-parent-id-column, param-parent-unique-column");

        String query  = String.format("SELECT %s AS c1 FROM %s WHERE %s = '%s'", params.get(columnKey),
                                        params.get(tableKey), params.get(uniqueKey), input);
        List<Object> result = new ArrayList<>();
        db.selectWithCallback(query, (ResultSet record, long index) -> {
            try {
                result.add(record.getObject("c1"));
            }
            catch (Exception ex) {
                logger.error("Error while Foreign key transform");
                ex.printStackTrace();
            }
        });
        return result.size() > 0 ? result.get(0) : null;
    }
}
