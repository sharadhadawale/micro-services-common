package com.rajanainart.common.transform;

import com.rajanainart.common.data.Database;

import java.util.Map;

public interface BaseTransform {
    Object transform(Database db, Object input, Map<String, String> params);
}
