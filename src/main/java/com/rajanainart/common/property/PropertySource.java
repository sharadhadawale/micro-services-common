package com.rajanainart.common.property;

import java.util.Map;

public interface PropertySource {
    Map<PropertyUtil.PropertyType, String> getProperties();
    int getPriority();
}
