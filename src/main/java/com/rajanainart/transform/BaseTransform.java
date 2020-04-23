package com.rajanainart.transform;

import com.rajanainart.data.Database;

import java.util.Map;

public interface BaseTransform {
    Object transform(Database db, Object input, Map<String, String> params);
}
