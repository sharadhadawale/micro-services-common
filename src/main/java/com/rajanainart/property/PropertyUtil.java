package com.rajanainart.property;

import com.rajanainart.config.AppContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.StreamSupport;

public class PropertyUtil {
    private static final Logger logger = LogManager.getLogger(PropertyUtil.class);

    public final static Map<String, PropertySource> PROPERTY_SOURCES = AppContext.getBeansOfType(PropertySource.class);

    public enum PropertyType {
        JDBC_DRIVER	 ("spring.datasource.driver-class-name"),
        JDBC_URL	 ("spring.datasource.url"),
        JDBC_USERNAME("spring.datasource.username"),
        JDBC_PASSWORD("spring.datasource.password"),

        HIBERNATE_SHOWSQL("spring.jpa.show-sql"),
        //HIBERNATE_HBM2DDL("hibernate.hbm2ddl.auto"),
        HIBERNATE_DIALECT("spring.jpa.properties.hibernate.dialect"),
        HIBERNATE_BEANS  ("hibernate.entity.beans"),

        APP_CONFIG_ENV      ("app.config.env"),
        APP_CONFIG_BASE     ("app.config.base"),
        APP_CONFIG_BASE_TEST("app.config.base.test"),
        APP_PATH_SEPERATOR	 ("app.path.seperator"),
        ACL_ALLOW_ORIGIN  	 ("access.control.allow.origin");

        private final String text;

        PropertyType(final String text) { this.text = text; }

        @Override
        public String toString() { return text; }

        public static Optional<PropertyType> getPropertyType(String value) {
            for (PropertyType p : PropertyType.values()) {
                if (p.toString().equalsIgnoreCase(value))
                    return Optional.of(p);
            }
            return Optional.empty();
        }
    }

    private static String getPropertySourceValue(PropertyType type) {
        String value 	 = "";
        int lastPriority = 0;
        for (Map.Entry<String, PropertySource> entry : PROPERTY_SOURCES.entrySet()) {
            if (!value.isEmpty() && entry.getValue().getPriority() <= lastPriority) continue;
            Map<PropertyType, String> properties = entry.getValue().getProperties();
            for (Map.Entry<PropertyType, String> entry1 : properties.entrySet()) {
                if (type == entry1.getKey()) {
                    value 		 = entry1.getValue();
                    lastPriority = entry.getValue().getPriority();
                    break;
                }
            }
        }
        return value;
    }

    public static Map<String, String> getAllProperties() {
        Map<String, String> properties = new HashMap<>();
        Environment env = AppContext.getApplicationContext().getEnvironment();
        final MutablePropertySources sources = ((AbstractEnvironment) env).getPropertySources();
        StreamSupport
                .stream  (sources.spliterator(), false)
                .filter  (ps -> ps instanceof EnumerablePropertySource)
                .map     (ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap (Arrays::stream)
                .distinct()
                .filter  (prop -> !(prop.contains("credentials") || prop.contains("password")))
                .forEach (prop -> {
                    try {
                        properties.put(prop, env.getProperty(prop));
                    }
                    catch(Exception ex) {
                        //reject the error
                    }
                });
        return properties;
    }

    public static Map<String, String> getAllProperties(String resourceName) {
        Map<String, String> properties = new HashMap<>();
        try (InputStream input = AppContext.class.getClassLoader().getResourceAsStream(resourceName)) {
            Properties prop = new Properties();
            if (input == null) {
                String log = String.format("Resource does not exist %s", resourceName);
                logger.error(log);
                return properties;
            }
            prop.load(input);
            prop.forEach((key, value) -> properties.put(String.valueOf(key), String.valueOf(value)));
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    public static String getPropertyValue(PropertyType type) {
        return getPropertySourceValue(type);
    }

    public static String getPropertyValue(PropertyType type, String defaultValue) {
        String value = getPropertySourceValue(type);
        return value.isEmpty() ? defaultValue : value;
    }

    public static String getPropertyValue(String name, String defaultValue) {
        return AppContext.getApplicationContext().getEnvironment().getProperty(name, defaultValue);
    }
}
