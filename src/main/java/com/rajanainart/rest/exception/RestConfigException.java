package com.rajanainart.rest.exception;

import com.rajanainart.rest.RestQueryConfig;

public class RestConfigException extends Exception {
    private RestQueryConfig config;
    private String message;

    public RestConfigException(RestQueryConfig config) {
        this.config = config;
    }

    public RestConfigException(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }
}
