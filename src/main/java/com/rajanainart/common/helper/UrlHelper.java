package com.rajanainart.common.helper;

import javax.servlet.http.HttpServletRequest;

public class UrlHelper {
    public static String getBaseUrl(HttpServletRequest request) {
        String baseUrl = String.format("%s://%s:%d%s",request.getScheme(),  request.getServerName(), request.getServerPort(), request.getContextPath());
        return baseUrl;
    }
}
