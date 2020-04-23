package com.rajanainart.template;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class ExpressionPart {
    private String value;
    private String text ;
    private Method underlyingMethod;
    private Field  underlyingField ;

    public String getValue() { return value; }
    public String getText () { return text ; }

    @JsonIgnore public Method getUnderlyingMethod() { return underlyingMethod; }
    @JsonIgnore public Field  getUnderlyingField () { return underlyingField ; }

    public ExpressionPart(String value, String text, Method method, Field field) {
        this.value = value;
        this.text  = text ;
        underlyingMethod = method;
        underlyingField  = field ;
    }
}
