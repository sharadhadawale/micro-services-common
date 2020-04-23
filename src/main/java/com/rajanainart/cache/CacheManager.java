package com.rajanainart.cache;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.BaseEntity;
import com.rajanainart.rest.*;

import java.util.List;
import java.util.Map;

public final class CacheManager {

    public final static String DEFAULT_CACHE_SERVER = "gemfire";
    public final static Map<String, BaseCacheServer> CACHE_SERVERS = AppContext.getBeansOfType(BaseCacheServer.class);

    private RestQueryConfig queryConfig;
    private RestQueryRequest queryRequest;

    private BaseCacheServer   server;
    private BaseEntityHandler handler;
    private BaseCacheServer.CacheExpiryValidationCallback callback;

    public CacheManager(RestQueryConfig queryConfig, RestQueryRequest queryRequest) throws CacheException {
        synchronized (CacheManager.class) {
            for (Map.Entry<String, BaseCacheServer> server : CACHE_SERVERS.entrySet())
                if (!server.getValue().isStarted())
                    server.getValue().start();
        }

        this.queryConfig  = queryConfig ;
        this.queryRequest = queryRequest;

        server   = getCacheServer(DEFAULT_CACHE_SERVER);
        handler  = RestController.getEntityHandler(queryConfig.getServiceName());
        callback = !(handler instanceof DefaultEntityHandler) ? handler::validateCacheExpiry : null;
    }

    public List<Map<String, Object>> getCachedMapRecords() {
        return server.getIfNotExpired(queryRequest, queryConfig.getExpiryMinutes(), callback);
    }

    public List<BaseEntity> getCachedEntities() {
        return server.getTypeIfNotExpired(queryRequest, queryConfig.getExpiryMinutes(), callback);
    }

    public CacheItem saveMapRecords(List<Map<String, Object>> records) {
        return server.save(queryRequest, records);
    }

    public CacheItem saveEntityRecords(List<BaseEntity> records) {
        return server.saveType(queryRequest, records);
    }

    public static BaseCacheServer getCacheServer(String key) {
        String key1 = String.format("cache-server-%s", key);
        String key2 = String.format("cache-server-%s", DEFAULT_CACHE_SERVER);
        return CACHE_SERVERS.containsKey(key1) ? CACHE_SERVERS.get(key1) : CACHE_SERVERS.get(key2);
    }
}
