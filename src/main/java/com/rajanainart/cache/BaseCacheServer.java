package com.rajanainart.cache;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.rest.RestQueryRequest;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface BaseCacheServer extends Closeable {
    String CACHE_REGION_NAME_KEY    = "cache.region.name";
    String CACHE_EXPIRY_MINUTES_KEY = "cache.expiry.minutes";
    String DEFAULT_REGION = "services-common-region";

    boolean isStarted();
    void start() throws CacheException;

    CacheItem save(RestQueryRequest queryRequest, List<Map<String, Object>> records);
    List<Map<String, Object>> getIfNotExpired(RestQueryRequest queryRequest,
                                              int expiryMinutes, CacheExpiryValidationCallback callback);

    CacheItem saveType(RestQueryRequest queryRequest, List<BaseEntity> records);
    List<BaseEntity> getTypeIfNotExpired(RestQueryRequest queryRequest,
                                         int expiryMinutes, CacheExpiryValidationCallback callback);

    <K, V> void saveKV(K keyInstance, List<V> valueInstance) throws IOException;
    <K, V> List<V> getKVIfNotExpired(K keyInstance, KVCacheExpiryValidationCallback callback)
                                    throws IOException, ClassNotFoundException;

    @FunctionalInterface
    interface CacheExpiryValidationCallback {
        boolean process(CacheItem item);
    }

    @FunctionalInterface
    interface KVCacheExpiryValidationCallback {
        <V> boolean process(List<V> values);
    }
}
