package com.rajanainart.common.rest;

import java.util.List;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.data.Database;

public interface BaseEntityHandler {

    <T extends BaseEntity> void setup(Class<T> entityClass, RestQueryConfig config, RestQueryRequest request, Database db);
    <T extends BaseEntity> List<T> fetchRestQueryResultSet(Class<T> entityClass, StringBuilder message);
    List<BaseEntity> executeQuery(StringBuilder message);

    String preValidateRestEntity ();
    <T extends BaseEntity> String postValidateRestEntity(List<T> input);
}
