package com.rajanainart.template;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.data.Database;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.helper.ReflectionHelper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        result        = parseAsSpelForJpaEntity (result);
        return parseAsSpelForHelperMethods(result);
    }

    public <T extends BaseEntity> String parseAsSpelAfterVariableAssignment(List<T> contextInstances, boolean isConditional) {
        try {
            String result = expression;
            result        = parseAsSpelForBaseEntity(contextInstances, result, isConditional);
            result        = parseAsSpelForJpaEntity (contextInstances, result, isConditional);
            return parseAsSpelForHelperMethods(contextInstances, result, isConditional);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return expression;
    }

    private static final String HELPER_METHOD_REGEX = "[a-zA-Z0-9]*\\.[a-zA-Z0-9]*\\([0-9\\.\\,]*\\)";
    private <T extends BaseEntity> String parseAsSpelForHelperMethods(List<T> contextInstances, String expression, boolean isConditional) {
        String  result  = expression;
        Pattern pattern = Pattern.compile(HELPER_METHOD_REGEX);
        Matcher matcher = pattern.matcher(result);
        while (matcher.find()) {
            String match = matcher.group();
            Expression             tempExp  = new Expression(match);
            ExpressionEvaluator<T> tempEval = new ExpressionEvaluator<>(tempExp, contextInstances, false);
            Object tempResult  = tempEval.parseAsObject();
            if (tempResult != null) {
                String finalResult = getSpelValueForType(tempResult.getClass(), tempResult, isConditional);
                result = result.replace(match, finalResult);
            }
            else
                result = result.replace(match, isConditional ? "''" : "");
        }
        return result;
    }

    private <T extends BaseEntity> String parseAsSpelForBaseEntity(List<T> contextInstances, String expression, boolean isConditional) throws Exception {
        String result = expression;
        for (T instance : contextInstances) {
            for (Map.Entry<String, Method> method : instance.getAnnotatedGetMethods().entrySet()) {
                String[] names = method.getKey().split("__");
                String   regex = String.format("\\[%s.%s\\]", instance.getBeanName(), names[0]);
                result         = result.replaceAll(regex,
                                                    getSpelValueForType(method.getValue().getReturnType(), method.getValue().invoke(instance), isConditional));
            }
        }
        return result;
    }

    private <T extends BaseEntity> String parseAsSpelForJpaEntity(List<T> contextInstances, String expression, boolean isConditional) throws Exception {
        String result = expression;
        for (T instance : contextInstances) {
            for (Map.Entry<String, Field> field : ReflectionHelper.getJpaTableColumns(instance.getClass()).entrySet()) {
                field.getValue().setAccessible(true);

                String regex = String.format("\\[%s.%s\\]", instance.getBeanName(), field.getKey());
                result       = result.replaceAll(regex, getSpelValueForType(field.getValue().getType(), field.getValue().get(instance), isConditional));
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

    private String parseAsSpelForHelperMethods(String expression) {
        String result = expression;
        for (Map.Entry<String, BaseEntity> methodClass : Template.METHOD_HELPERS.entrySet()) {
            for (Method method : methodClass.getValue().getClass().getMethods()) {
                String regex   = String.format("%s.%s", methodClass.getKey(), method.getName());
                String replace = String.format("instances['%s'].%s", methodClass.getKey(), method.getName());
                result         = result.replaceAll(regex, replace);
            }
        }
        return result;
    }


    public static String getSpelValueForType(Class<?> typeName, Object value, boolean isConditional) {
        switch (typeName.getName()) {
            case Database.BYTE_NAME:
            case Database.INTEGER_NAME:
            case Database.LONG_NAME:
            case Database.FLOAT_NAME:
            case Database.DOUBLE_NAME:
            case Database.BYTE_CLASS_NAME:
            case Database.INTEGER_CLASS_NAME:
            case Database.LONG_CLASS_NAME:
            case Database.FLOAT_CLASS_NAME:
            case Database.DOUBLE_CLASS_NAME:
                return String.valueOf(value);
            default:
                String text = MiscHelper.convertRegexCharsToText(String.valueOf(value));
                return isConditional ? String.format("'%s'", text) : text;
        }
    }

    public static List<String> getSystemMethods() {
        List<String> methods = new ArrayList<>();
        for (Map.Entry<String, BaseEntity> methodClass : Template.METHOD_HELPERS.entrySet())
            methods.addAll(ReflectionHelper.getClassMethods(methodClass.getValue().getClass(), methodClass.getKey()));
        methods.addAll(ReflectionHelper.getClassMethods(Math.class, ""));

        return methods;
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
        parts.sort(Comparator.comparing(ExpressionPart::getText));
        return parts;
    }
}
