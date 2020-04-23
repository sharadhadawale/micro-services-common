package com.rajanainart.data.nosql;

import com.rajanainart.data.provider.DbSpecificProvider;
import com.rajanainart.rest.RestQueryConfig;
import com.rajanainart.rest.exception.RestConfigException;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BaseNoSqlDataProvider extends Closeable, DbSpecificProvider {
    void open (NoSqlConfig noSqlConfig, RestQueryConfig queryConfig);
    void close();
    String bulkUpdate(List<Map<String, Object>> records) throws RestConfigException, IOException;
    String bulkDelete(List<Map<String, Object>> records) throws RestConfigException, IOException;
}
