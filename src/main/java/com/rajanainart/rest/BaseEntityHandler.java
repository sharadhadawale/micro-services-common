package com.rajanainart.rest;

import java.util.List;
import java.util.Map;

import com.rajanainart.cache.CacheItem;
import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;

public interface BaseEntityHandler {

    <T extends BaseEntity> void setup(Class<T> entityClass, RestQueryConfig config, RestQueryRequest request, Database db);
    <T extends BaseEntity> List<T> fetchRestQueryResultSet(Class<T> entityClass, StringBuilder message);
    List<BaseEntity> executeQuery(StringBuilder message);
    List<Map<String, Object>> fetchRestQueryAsMap();

    String preValidateRestEntity ();
    <T extends BaseEntity> String postValidateRestEntity(List<T> input);

    <T extends BaseEntity> List<T> executePostAction(Class<T> clazz, List<T> input);
    List<Map<String, Object>> executePostAction(List<Map<String, Object>> input);

    boolean validateCacheExpiry(CacheItem item);
}
