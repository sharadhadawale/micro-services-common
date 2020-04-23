package com.rajanainart.cache;

public class CacheException extends Exception {
    public CacheException(String message, Exception exception) {
        super(message, exception);
    }
}
