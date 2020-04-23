package com.rajanainart.config;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

import com.rajanainart.data.BaseEntity;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@SuppressWarnings("unchecked")
public class AppContext implements ApplicationContextAware, WebMvcConfigurer {
    private static ApplicationContext context = null;

    public static ApplicationContext getApplicationContext() {
        return context;
    }

    @Override
    public void setApplicationContext(ApplicationContext c) throws BeansException {
        init(c);
    }

    private static void init(ApplicationContext context) {
        AppContext.context = context;
    }

    public static <T> Map<String, T> getBeansOfType(Class<T> clazz) {
        return context.getBeansOfType(clazz);
    }

    public static <T extends BaseEntity, A extends Annotation> Map<String, T> getBeansOfType(Class<T> clazz, Class<A> annotation) {
        Map<String, T> beans  = context.getBeansOfType(clazz);
        Map<String, T> result = new HashMap<>();
        for (Map.Entry<String, T> bean : beans.entrySet()) {
            Annotation a = bean.getValue().getClass().getAnnotation(annotation);
            if (a != null)
                result.put(bean.getKey(), bean.getValue());
        }
        return result;
    }

    public static <T> T getBeanOfType(Class<T> clazz) {
        return context.getBean(clazz);
    }

    public static <T> T getBeanOfType(Class<T> clazz, String beanName) {
        Map<String, T> list = getBeansOfType(clazz);
        if (list.containsKey(beanName))
            return list.get(beanName);
        return null;
    }

    public static Class<?> getClassType(String beanName) {
        return context.getType(beanName);
    }

    public static <T extends BaseEntity> Class<T> getClassTypeOf(String beanName) {
        return  (Class<T>)AppContext.getClassType(beanName);
    }

    public static <T> Map<String, Class<T>> getClassTypesOf(Class<T> baseClass) {
        Map<String, Class<T>> types = new HashMap<>();
        Map<String, T> beans = context.getBeansOfType(baseClass);
        for (Map.Entry<String, T> entry: beans.entrySet())
            types.put(entry.getKey(), (Class<T>)entry.getValue().getClass());
        return types;
    }

    public static <T, V extends XmlConfig> Class<V> getClassOfType(Class<T> baseClass, String beanName) {
        V bean = (V)getBeanOfType(baseClass, beanName);
        if (bean != null)
            return (Class<V>)bean.getClass();
        return null;
    }
}
