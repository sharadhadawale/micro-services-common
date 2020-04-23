package com.rajanainart.property;

import com.rajanainart.config.AppContext;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.json.JsonSanitizer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class EnvPropertySource implements PropertySource {
    private static final Logger logger = LogManager.getLogger(EnvPropertySource.class);

    public final String APP_PROPERTIES = "APP_PROPERTIES";

    public Map<PropertyUtil.PropertyType, String> getProperties() {
        Map<PropertyUtil.PropertyType, String> properties = new HashMap<>();
        String value = AppContext.getApplicationContext().getEnvironment().getProperty(APP_PROPERTIES, "");
        if (!value.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                value = JsonSanitizer.sanitize(value);
                Map<String, String> jsonProperties = mapper.readValue(value, new TypeReference<Map<String, String>>() {});
                if (jsonProperties != null) {
                    for (Map.Entry<String, String> entry : jsonProperties.entrySet()) {
                        Optional<PropertyUtil.PropertyType> p = PropertyUtil.PropertyType.getPropertyType(entry.getKey());
                        if (p.isPresent())
                            properties.put(p.get(), entry.getValue());
                    }
                }
            }
            catch (Exception ex2) {
                logger.error(String.format("Exception while parsing app properties from environment value:%s %n %s", APP_PROPERTIES, ex2.getMessage()));
            }
        }
        return properties;
    }

    public int getPriority() { return 10; }
}
