package com.rajanainart.data;

import java.lang.annotation.ElementType;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DbCol {
    String name();
    String display() default  "";
    BaseMessageColumn.ColumnType type();
}
