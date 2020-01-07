package com.rajanainart.common.data;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import com.rajanainart.common.helper.ReflectionHelper;
import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class BaseEntity {
    public static final String DAFAULT_DATE_OUTPUT_FORMAT    = "MM/dd/yyyy";
    public static final String DAFAULT_ORACLE_DATE_FORMAT    = "dd-MMM-yyyy";
    public static final String DEFAULT_NUMERIC_OUTPUT_FORMAT = "%.2f";

    private double id = 0;

    @JsonIgnore
    public Optional<Long> getId() {
        if (id != 0)
            return Optional.of((long)id);
        return Optional.empty();
    }

    @DbCol(name = "ID", type = BaseMessageColumn.ColumnType.NUMERIC)
    public void setId(double id) {
        this.id = id;
    }

    @JsonIgnore
    public Map<String, Method> getAnnotatedSetMethods() {
        return ReflectionHelper.getAnnotatedSetMethods(this.getClass());
    }

    @JsonIgnore
    public Map<String, Method> getAnnotatedGetMethods() {
        return ReflectionHelper.getAnnotatedGetMethods(this.getClass());
    }
}
