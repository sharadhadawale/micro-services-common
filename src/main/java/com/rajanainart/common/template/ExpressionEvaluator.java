package com.rajanainart.common.template;

import com.rajanainart.common.data.BaseEntity;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExpressionEvaluator<T extends BaseEntity> {
    private boolean           assignVariables  ;
    private List<T>           contextInstances ;
    private Expression        expression       ;
    private ExpressionParser  expressionParser ;
    private EvaluationContext evaluationContext;
    private org.springframework.expression.Expression springExpression;

    public EvaluationContext getEvaluationContext() { return evaluationContext; }
    public ExpressionParser  getExpressionParser () { return expressionParser ; }

    public ExpressionEvaluator(Expression expression, List<T> contextInstances, boolean assignVariables) {
        this.contextInstances = contextInstances;
        this.assignVariables  = assignVariables ;
        this.expression       = expression      ;
        init();
    }

    private void init() {
        Context context = new Context();
        for (T instance :  contextInstances)
            context.instances.put(instance.getBeanName(), instance);

        this.expressionParser  = new SpelExpressionParser();
        this.evaluationContext = new StandardEvaluationContext(context);
    }

    public boolean parseAsBoolean() {
        this.springExpression = assignVariables ? expressionParser.parseExpression(expression.parseAsSpelAfterVariableAssignment(contextInstances)) :
                                                  expressionParser.parseExpression(expression.getRawExpression());
        String value = String.valueOf(springExpression.getValue(evaluationContext));
        return Boolean.valueOf(value);
    }

    public String parseAsString() {
        this.springExpression = assignVariables ? expressionParser.parseExpression("'"+expression.parseAsSpelAfterVariableAssignment(contextInstances)+"'") :
                                                  expressionParser.parseExpression(expression.getRawExpression());
        return String.valueOf(springExpression.getValue(evaluationContext));
    }

    public <N extends Number> N parseAsType(Class<N> clazz) {
        this.springExpression = assignVariables ? expressionParser.parseExpression(expression.parseAsSpelAfterVariableAssignment(contextInstances)) :
                                                  expressionParser.parseExpression(expression.getRawExpression());
        return clazz.cast(springExpression.getValue(evaluationContext));
    }

    public class Context {
        public Map<String, T> instances = new HashMap<>();
    }
}
