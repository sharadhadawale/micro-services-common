package com.rajanainart.common.property;

import com.rajanainart.common.config.AppContext;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AppPropertySource implements PropertySource {

    public Map<PropertyUtil.PropertyType, String> getProperties() {
        Map<PropertyUtil.PropertyType, String> properties = new HashMap<>();
        for (PropertyUtil.PropertyType p : PropertyUtil.PropertyType.values())
            properties.put(p, AppContext.getApplicationContext().getEnvironment().getProperty(p.toString(), ""));
        return properties;
    }

    public int getPriority() { return 1; }
}
