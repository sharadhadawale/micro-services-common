package com.rajanainart.helper;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.BaseEntity;

import com.rajanainart.data.DbCol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Table;

public class ReflectionHelper {
    private static final Logger log = LoggerFactory.getLogger(ReflectionHelper.class);

    private ReflectionHelper() {}

    private static Map<String, Map<String, Method>> methods    = new HashMap<>();
    private static Map<String, Map<String, Method>> getMethods = new HashMap<>();

    public static <T extends BaseEntity, A extends Annotation> boolean isTypeAnnotatedWith(Class<T> clazz, Class<A> annotation) {
        A annotated = clazz.getAnnotation(annotation);
        return annotated != null;
    }

    public static List<String> getClassMethods(Class<?> clazz, String alias) {
        List<String> result = new ArrayList<>();
        String name = alias.isEmpty() ? clazz.getSimpleName() : alias;
        for (Method method : clazz.getDeclaredMethods()) {
            StringBuilder builder = new StringBuilder();
            int index = 0;
            builder.append(String.format("%s.%s", name, method.getName())).append("(");
            for (Parameter p : method.getParameters())
                builder.append(String.format("%s%s %s", index++!=0 ? ", " : "", p.getType().getName(), p.getName()));
            builder.append(")");
            result.add(builder.toString());
        }
        return result;
    }

    public static String getJpaTableName(Class<?> clazz) {
        Table annotation = clazz.getAnnotation(Table.class);
        return annotation != null ? annotation.name() : "";
    }

    public static Map<String, Field> getJpaTableColumns(Class<?> clazz) {
        Map<String, Field> fields = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            Column column = field.getDeclaredAnnotation(Column.class);
            if (column != null) {
                field.setAccessible(true);
                fields.put(column.name(), field);
            }
        }
        return fields;
    }

    public static String getJpaCondition(Class<?> clazz, Map<String, Object> conditions) {
        StringBuilder condition = new StringBuilder();
        condition.append("1=1");
        try {
            for (Map.Entry<String, Object> key : conditions.entrySet()) {
                Field field = clazz.getDeclaredField(key.getKey());
                if (field != null) {
                    Column column = field.getAnnotation(Column.class);
                    condition.append(String.format(" AND %s = '%s'", column.name(), key.getValue()));
                }
            }
        }
        catch(Exception ex) {
            log.error("Error while converting conditions Map to conditions string");
            ex.printStackTrace();
        }
        return condition.toString();
    }

    public static Object getInstanceFromMap(Class<?> clazz, Map<String, Object> record) {
        try {
            Object instance = clazz.newInstance();

            for (Map.Entry<String, Object> key : record.entrySet()) {
                String methodName = String.format("set%s", StringUtils.capitalize(key.getKey()));
                for (Method method : clazz.getMethods()) {
                    if (!method.getName().equals(methodName)) continue;
                    if (!method.getReturnType().getSimpleName().equalsIgnoreCase(Void.class.getSimpleName())) continue;
                    if (method.getModifiers() != Modifier.PUBLIC) continue;

                    method.invoke(instance, key.getValue());
                    break;
                }
            }
            return instance;
        }
        catch(Exception ex) {
            log.error("Error while converting Map to Object");
            ex.printStackTrace();
        }
        return null;
    }

    public static <A extends Annotation> Annotation getAnnotation(Method method, Class<A> annotation) {
        for (Annotation a : method.getDeclaredAnnotations()) {
            if (annotation.isAssignableFrom(a.annotationType()))
                return a;
        }
        return null;
    }

    public static <T, A extends Annotation> Map<String, Method> getAnnotatedSetMethods(Class<T> clazz, Class<A> annotation) {
        if (!methods.containsKey(clazz.getName()) || methods.get(clazz.getName()).size() == 0) {
            Map<String, Method> m = new HashMap<>();
            for (Method method : clazz.getMethods()) {
                if (!method.getReturnType().getSimpleName().equalsIgnoreCase(Void.class.getSimpleName())) continue;
                if (method.getModifiers() != Modifier.PUBLIC) continue;

                for (Annotation a : method.getDeclaredAnnotations()) {
                    if (annotation.isAssignableFrom(a.annotationType())) {
                        String name = String.valueOf(AnnotationUtils.getValue(a, "name"));
                        m.put(name, method);
                    }
                }
            }
            methods.put(clazz.getName(), m);
        }
        return methods.get(clazz.getName());
    }

    public static <T, A extends Annotation> Map<String, Method> getAnnotatedGetMethods(Class<T> clazz, Class<A> annotation) {
        if (!getMethods.containsKey(clazz.getName()) || getMethods.get(clazz.getName()).size() == 0) {
            Map<String, Method> m = new HashMap<>();
            for (Method method : clazz.getMethods()) {
                if (method.getReturnType().getSimpleName().equalsIgnoreCase(Void.class.getSimpleName())) continue;
                if (method.getModifiers() != Modifier.PUBLIC) continue;

                for (Annotation a : method.getDeclaredAnnotations()) {
                    if (annotation.isAssignableFrom(a.annotationType())) {
                         String name = String.valueOf(AnnotationUtils.getValue(a, "name"));
                         m.put(name, method);
                    }
                }
            }
            getMethods.put(clazz.getName(), m);
        }
        return getMethods.get(clazz.getName());
    }

    public static <T extends BaseEntity> Map<String, Method> getAnnotatedSetMethods(Class<T> clazz) {
        if (!methods.containsKey(clazz.getName()) || methods.get(clazz.getName()).size() == 0) {
            Map<String, Method> m = new HashMap<>();
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(DbCol.class)) continue;
                if (!method.getReturnType().getSimpleName().equalsIgnoreCase(Void.class.getSimpleName())) continue;
                if (method.getModifiers() != Modifier.PUBLIC) continue;

                DbCol column = method.getAnnotation(DbCol.class);
                if (column  != null)
                    m.put(column.name(), method);
            }
            methods.put(clazz.getName(), m);
        }
        return methods.get(clazz.getName());
    }

    public static <T extends BaseEntity> Map<String, Method> getAnnotatedGetMethods(Class<T> clazz) {
        if (!getMethods.containsKey(clazz.getName()) || getMethods.get(clazz.getName()).size() == 0) {
            Map<String, Method> m = new HashMap<>();
            for (Method method : clazz.getMethods()) {
                if (!method.isAnnotationPresent(DbCol.class)) continue;
                if (method.getReturnType().getSimpleName().equalsIgnoreCase(Void.class.getSimpleName())) continue;
                if (method.getModifiers() != Modifier.PUBLIC) continue;

                DbCol column = method.getAnnotation(DbCol.class);
                if (column  != null) {
                    String name = column.name();
                    if (!column.display().isEmpty())
                        name = String.format("%s__%s", column.name(), column.display());
                    m.put(name, method);
                }
            }
            getMethods.put(clazz.getName(), m);
        }
        return getMethods.get(clazz.getName());
    }

    public static <T extends BaseEntity> Map<String, Method> getAnnotatedSetMethods(String beanName) {
        Class<T> clazz = AppContext.getClassTypeOf(beanName);
        return getAnnotatedSetMethods(clazz);
    }

    public static <T extends BaseEntity> Map<String, Method> getAnnotatedGetMethods(String beanName) {
        Class<T> clazz = AppContext.getClassTypeOf(beanName);
        return getAnnotatedGetMethods(clazz);
    }

    public static Object getMethodReturnValue(Method method, Object instance, Object ... arguments) {
        try {
            return method.invoke(instance, arguments);
        }
        catch(Exception ex) {
            return null;
        }
    }

    public static <T extends Number> T convertStringToNumber(Class<T> tClass, String value) {
        if (tClass.isAssignableFrom(Byte.class))
            return (T)Byte.valueOf(value);
        else if (tClass.isAssignableFrom(Short.class))
            return (T)Short.valueOf(value);
        else if (tClass.isAssignableFrom(Integer.class))
            return (T)Integer.valueOf(value);
        else if (tClass.isAssignableFrom(Long.class))
            return (T)Long.valueOf(value);
        else if (tClass.isAssignableFrom(Float.class))
            return (T)Float.valueOf(value);
        else if (tClass.isAssignableFrom(Double.class))
            return (T)Double.valueOf(value);

        return tClass.cast(0);
    }
}
