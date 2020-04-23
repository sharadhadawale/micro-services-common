package com.rajanainart.cache;

import com.rajanainart.data.BaseEntity;
import com.rajanainart.helper.JsonParser;
import com.rajanainart.helper.SerializeHelper;
import com.rajanainart.property.PropertyUtil;
import com.rajanainart.rest.RestQueryRequest;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.client.ClientCache;
import org.apache.geode.cache.client.ClientCacheFactory;
import org.apache.geode.cache.client.ClientRegionShortcut;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component("cache-server-gemfire")
@Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
public final class GemfireCacheServer implements BaseCacheServer {

    public final Pattern LOCATOR_PATTERN = Pattern.compile("(.*)\\[(\\d*)\\]");

    private List<URI> locators;

    private ClientCache client;
    private Region      region;

    public List<URI> getLocators() { return locators; }

    private static boolean started = false;

    @Override
    public boolean isStarted() { return started; }

    @Override
    public void start() throws CacheException {
        if (started) return;

        initialize();

        Properties properties = new Properties();
        properties.setProperty("security-client-auth-init", String.format("%s.create", CacheAuth.class.getName()));
        ClientCacheFactory factory = new ClientCacheFactory(properties);

        for (URI locator : locators)
            factory.addPoolLocator(locator.getHost(), locator.getPort());

        String regionName = PropertyUtil.getPropertyValue(CACHE_REGION_NAME_KEY, DEFAULT_REGION);
        client  = factory.create();
        region  = client.createClientRegionFactory(ClientRegionShortcut.PROXY).create(regionName);
        started = true;
    }

    private void initialize() throws CacheException {
        try {
            String     vcap   = System.getenv().get("VCAP_SERVICES");
            JsonParser parser = new JsonParser(vcap);
            Map    properties = (Map)parser.getValueAsList("p-cloudcache").get(0);
            Map   credentials = JsonParser.getValueAsMap(properties, "credentials");
            Map          user = (Map)JsonParser.getValueAsList(credentials, "users").get(0);

            List<String> locators = JsonParser.getValueAsList(credentials, "locators");
            this.locators = getLocators(locators);
        }
        catch (Exception ex) {
            throw new CacheException(ex.getLocalizedMessage(), ex);
        }
    }

    private List<URI> getLocators(List<String> locators) throws URISyntaxException {
        List<URI> list = new ArrayList<>();
        for (String locator : locators) {
            Matcher m = LOCATOR_PATTERN.matcher(locator);
            if (m.matches()) {
                list.add(new URI("locator://" + m.group(1) + ":" + m.group(2)));
            }
        }
        return list;
    }

    @Override
    public CacheItem save(RestQueryRequest queryRequest, List<Map<String, Object>> records) {
        CacheItem cacheItem = new CacheItem(queryRequest, records);
        String key   = cacheItem.buildCacheKey  ();
        String value = cacheItem.buildCacheValue();

        region.put(key, value);
        return cacheItem;
    }

    @Override
    public List<Map<String, Object>> getIfNotExpired(RestQueryRequest queryRequest,
                                                     int expiryMinutes, CacheExpiryValidationCallback callback) {
        String key   = CacheItem.buildCacheKey(queryRequest);
        Object value = region.get(key);
        if (value != null) {
            CacheItem item = CacheItem.deserialize(String.valueOf(value));
            if (callback != null) {
                if (!callback.process(item))
                    return item.getMapRecords();
            }
            else {
                if (!item.isExpired(expiryMinutes))
                    return item.getMapRecords();
            }
        }
        return null;
    }

    @Override
    public CacheItem saveType(RestQueryRequest queryRequest, List<BaseEntity> records) {
        CacheItem cacheItem = new CacheItem(records, queryRequest);
        String key   = cacheItem.buildCacheKey  ();
        String value = cacheItem.buildCacheValue();

        region.put(key, value);
        return cacheItem;
    }

    @Override
    public List<BaseEntity> getTypeIfNotExpired(RestQueryRequest queryRequest,
                                                int expiryMinutes, CacheExpiryValidationCallback callback) {
        String key   = CacheItem.buildCacheKey(queryRequest);
        Object value = region.get(key);
        if (value != null) {
            CacheItem item = CacheItem.deserialize(String.valueOf(value));
            if (callback != null) {
                if (!callback.process(item))
                    return item.getEntityRecords();
            }
            else {
                if (!item.isExpired(expiryMinutes))
                    return item.getEntityRecords();
            }
        }
        return null;
    }

    @Override
    public <K, V> void saveKV(K keyInstance, List<V> valueInstance) throws IOException {
        String key   = SerializeHelper.serialize(keyInstance  );
        String value = SerializeHelper.serialize(valueInstance);
        region.put(key, value);
    }

    @Override
    public <K, V> List<V> getKVIfNotExpired(K keyInstance, KVCacheExpiryValidationCallback callback)
                                            throws IOException, ClassNotFoundException {
        String key   = SerializeHelper.serialize(keyInstance);
        Object value = region.get(key);
        if (value != null) {
            List<V> values = SerializeHelper.deserialize(String.valueOf(value));
            if (callback != null) {
                if (!callback.process(values))
                    return values;
            }
            else
                return values;
        }
        return null;
    }

    @Override
    public void close() {
        if (client != null) client.close(true);
        if (region != null) region.close();
    }
}
