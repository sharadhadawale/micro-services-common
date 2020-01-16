package com.rajanainart.common.template;

import com.rajanainart.common.data.BaseEntity;
import com.rajanainart.common.helper.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Expression {
    private static List<ExpressionPart> parts = new ArrayList<>();

    public static final String[] OPERATORS = { "+", "-", "*", "/", "%", "==", "!=", "<", "<=", ">", ">=", "&&", "||" };

    private String expression;

    public String getRawExpression() { return expression; }

    public Expression(String expression) {
        this.expression = expression;
    }

    public String parseAsSpel() {
        String result = expression;
        result        = parseAsSpelForBaseEntity(result);
        return parseAsSpelForJpaEntity(result);
    }

    public <T extends BaseEntity> String parseAsSpelAfterVariableAssignment(List<T> contextInstances) {
        try {
            String result = expression;
            result        = parseAsSpelForBaseEntity(contextInstances, result);
            return parseAsSpelForJpaEntity(contextInstances, result);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return expression;
    }

    private <T extends BaseEntity> String parseAsSpelForBaseEntity(List<T> contextInstances, String expression) throws Exception {
        String result = expression;
        for (T instance : contextInstances) {
            for (Map.Entry<String, Method> method : instance.getAnnotatedGetMethods().entrySet()) {
                String[] names = method.getKey().split("__");
                String   regex = String.format("\\[%s.%s\\]", instance.getBeanName(), names[0]);
                result         = result.replaceAll(regex, String.valueOf(method.getValue().invoke(instance)));
            }
        }
        return result;
    }

    private <T extends BaseEntity> String parseAsSpelForJpaEntity(List<T> contextInstances, String expression) throws Exception {
        String result = expression;
        for (T instance : contextInstances) {
            for (Map.Entry<String, Field> field : ReflectionHelper.getJpaTableColumns(instance.getClass()).entrySet()) {
                field.getValue().setAccessible(true);

                String regex = String.format("\\[%s.%s\\]", instance.getBeanName(), field.getKey());
                result       = result.replaceAll(regex, String.valueOf(field.getValue().get(instance)));
            }
        }
        return result;
    }

    private String parseAsSpelForBaseEntity(String expression) {
        String result = expression;
        for (Map.Entry<String, BaseEntity> source : Template.TEMPLATE_SOURCES.entrySet()) {
            for (Map.Entry<String, Method> method : source.getValue().getAnnotatedGetMethods().entrySet()) {
                String[] names = method.getKey().split("__");
                String   regex = String.format("\\[%s.%s\\]", source.getKey(), names[0]);
                String replace = String.format("instances['%s'].%s()", source.getKey(), method.getValue().getName());
                result         = result.replaceAll(regex, replace);
            }
        }
        return result;
    }

    private String parseAsSpelForJpaEntity(String expression) {
        String result = expression;
        for (Map.Entry<String, BaseEntity> source : Template.TEMPLATE_SOURCES.entrySet()) {
            for (Map.Entry<String, Field> field : ReflectionHelper.getJpaTableColumns(source.getValue().getClass()).entrySet()) {
                field.getValue().setAccessible(true);

                String regex   = String.format("\\[%s.%s\\]", source.getKey(), field.getKey());
                String replace = String.format("instances['%s'].%s", source.getKey(), field.getValue().getName());
                result         = result.replaceAll(regex, replace);
            }
        }
        return result;
    }

    public static List<String> getSystemMethods() {
        return ReflectionHelper.getAllClassMethods(Math.class);
    }

    public static List<ExpressionPart> getExpressionParts() {
        if (parts.size() > 0) return parts;

        for (Map.Entry<String, BaseEntity> source : Template.TEMPLATE_SOURCES.entrySet()) {
            for (Map.Entry<String, Method> method : source.getValue().getAnnotatedGetMethods().entrySet()) {
                String[] names = method.getKey().split("__");
                String   value = String.format("[%s.%s]", source.getKey(), names[0]);
                String   text  = names.length > 1 ? names[1] : value;
                parts.add(new ExpressionPart(value, text, method.getValue(), null));
            }
            for (Map.Entry<String, Field> field : ReflectionHelper.getJpaTableColumns(source.getValue().getClass()).entrySet()) {
                String value = String.format("[%s.%s]", source.getKey(), field.getKey());
                parts.add(new ExpressionPart(value, value, null, field.getValue()));
            }
        }
        return parts;
    }
}
