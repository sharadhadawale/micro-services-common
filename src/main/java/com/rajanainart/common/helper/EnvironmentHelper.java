package com.rajanainart.common.helper;

import com.rajanainart.common.config.AppConfig;
import org.springframework.web.util.HtmlUtils;

public class EnvironmentHelper {

    private EnvironmentHelper() {}

    public static boolean isVariableAvailable(String env) {
        String value = System.getenv(env);
        return value != null;
    }

    public static String getValueAsHtmlEscaped(String env) {
        String value = getValueAsString(env, "");
        return HtmlUtils.htmlEscape(value);
    }

    public static boolean getValueAsBoolean(String env, boolean defaultValue) {
        String value = System.getenv(env);
        if (value != null && !value.isEmpty())
            return Boolean.parseBoolean(value);
        return defaultValue;
    }

    public static String getValueAsString(String env, String defaultValue) {
        String value = System.getenv(env);
        if (value != null && !value.isEmpty())
            return value;
        return defaultValue;
    }

    public static String getHomePath() {
        String value = getValueAsString("HOMEDRIVE", "")+getValueAsString("HOMEPATH", "");
        if (value.isEmpty())
            value = getValueAsString("HOME", "");
        if (value.isEmpty())
            value = FileHelper.getAppBasePath(AppConfig.getResourceFilePath());
        return value;
    }
}
