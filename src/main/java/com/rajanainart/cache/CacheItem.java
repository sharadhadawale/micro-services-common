package com.rajanainart.cache;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.helper.MiscHelper;
import com.rajanainart.helper.SerializeHelper;
import com.rajanainart.property.PropertyUtil;
import com.rajanainart.rest.RestQueryRequest;

import java.io.IOException;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

public final class CacheItem implements Serializable {
    public enum CacheItemType { NO_ITEM, BASE_ENTITY_LIST, MAP_LIST }

    private RestQueryRequest queryRequest;
    private LocalDateTime    createdDateTime = LocalDateTime.now();
    private List<BaseEntity> entityRecords;
    private List<Map<String, Object>> mapRecords;
    private CacheItemType cacheItemType = CacheItemType.NO_ITEM;
    private int expiryMinutes = 120;

    public CacheItem(RestQueryRequest queryRequest, List<Map<String, Object>> mapRecords) {
        this.mapRecords    = mapRecords;
        this.queryRequest  = queryRequest;
        this.cacheItemType = CacheItemType.MAP_LIST;
        init();
    }

    public CacheItem(List<BaseEntity> entityRecords, RestQueryRequest queryRequest) {
        this.entityRecords = entityRecords;
        this.queryRequest  = queryRequest;
        this.cacheItemType = CacheItemType.BASE_ENTITY_LIST;
        init();
    }

    private void init() {
        String mins   = PropertyUtil.getPropertyValue(GemfireCacheServer.CACHE_EXPIRY_MINUTES_KEY, String.valueOf(expiryMinutes));
        expiryMinutes = MiscHelper.convertStringToInt(mins);
    }

    public LocalDateTime        getCreatedDateTime() { return createdDateTime; }
    public List<BaseEntity>       getEntityRecords() { return entityRecords  ; }
    public List<Map<String, Object>> getMapRecords() { return mapRecords     ; }
    public CacheItemType          getCacheItemType() { return cacheItemType  ; }
    public int getExpiryMinutes() { return expiryMinutes; }

    public String buildCacheKey() {
        return buildCacheKey(queryRequest);
    }

    public String buildCacheValue() {
        return serialize(this);
    }

    public boolean isExpired(int expiryMinutes) {
        int  mins    = expiryMinutes > 0 ? expiryMinutes : this.expiryMinutes;
        long elapsed = createdDateTime.until(LocalDateTime.now(), ChronoUnit.MINUTES);

        return elapsed > mins;
    }

    public static String buildCacheKey(RestQueryRequest queryRequest) {
        try {
            return SerializeHelper.serialize(queryRequest);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return convertMapToString(queryRequest.getParams());
        }
    }

    public static String serialize(CacheItem item) {
        try {
            return SerializeHelper.serialize(item);
        }
        catch (IOException ex) {
            ex.printStackTrace();
            return String.format("Invalid Object:%s", ex.getLocalizedMessage());
        }
    }

    public static CacheItem deserialize(String serialized) {
        try {
            return SerializeHelper.deserialize(serialized);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    private static String convertMapToString(Map<String, String> map) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : map.entrySet())
            builder.append(String.format("%s%s.%s", builder.length() != 0 ? "_" : "", entry.getKey(), entry.getValue()));
        return builder.toString();
    }
}
