package com.rajanainart.property;

import java.util.Map;

public interface PropertySource {
    Map<PropertyUtil.PropertyType, String> getProperties();
    int getPriority();
}
