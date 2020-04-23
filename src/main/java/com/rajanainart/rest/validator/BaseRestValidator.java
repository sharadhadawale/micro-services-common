package com.rajanainart.rest.validator;

import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.helper.ReflectionHelper;
import com.rajanainart.rest.RestQueryConfig;

@SuppressWarnings("unchecked")

public interface BaseRestValidator {

    String validate(RestQueryConfig config, RestQueryConfig.ValidationExecutionType type, Map<String, String> params, Map<String, Object> objectParams);

    default <T extends BaseEntity, V> Map<String, V> buildResultSetReturnValues(Class<T> sourceType, Class<V> targetType, ResultSet record) {
        Map<String, V> methodValues = new HashMap<>();
        Map<String, Method> methods = ReflectionHelper.getAnnotatedGetMethods(sourceType);
        for (String key : methods.keySet()) {
            try {
                methodValues.put(key, (V)record.getString(key));
            }
            catch(SQLException ex) {
                methodValues.put(key, null);
            }
        }
        return methodValues;
    }

    default <T extends BaseEntity, V> Map<String, V> buildMethodReturnValues(Class<V> clazz, T instance) {
        Map<String, V> methodValues = new HashMap<>();
        Map<String, Method> methods = ReflectionHelper.getAnnotatedGetMethods(instance.getClass());
        for (Map.Entry<String, Method> entry : methods.entrySet()) {
            Object value = ReflectionHelper.getMethodReturnValue(entry.getValue(), instance);
            switch (clazz.getName()) {
                case "java.lang.Double":
                    methodValues.put(entry.getKey(), (V)Double.valueOf(String.valueOf(value)));
                    break;
                case "java.lang.Integer":
                    methodValues.put(entry.getKey(), (V)Integer.valueOf(String.valueOf(value)));
                    break;
                default:
                    methodValues.put(entry.getKey(), (V)String.valueOf(value));
                    break;
            }
        }
        return methodValues;
    }
}
