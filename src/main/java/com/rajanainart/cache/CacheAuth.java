package com.rajanainart.cache;

import com.rajanainart.helper.JsonParser;
import org.apache.geode.distributed.DistributedMember;
import org.apache.geode.security.AuthInitialize;
import org.apache.geode.security.AuthenticationFailedException;

import java.util.Map;
import java.util.Properties;

public class CacheAuth implements AuthInitialize {

    public static final String USER_NAME = "security-username";
    public static final String PASSWORD  = "security-password";

    private String userName = "";
    private String password = "";

    public static AuthInitialize create() {
        return new CacheAuth();
    }

    public String getUserName() { return userName; }

    public CacheAuth() {
        initialize();
    }

    private void initialize() {
        try {
            String     vcap   = System.getenv().get("VCAP_SERVICES");
            JsonParser parser = new JsonParser(vcap);
            Map properties = (Map)parser.getValueAsList("p-cloudcache").get(0);
            Map   credentials = JsonParser.getValueAsMap(properties, "credentials");
            Map          user = (Map)JsonParser.getValueAsList(credentials, "users").get(0);
            this.userName     = JsonParser.getValueAsString(user, "username");
            this.password     = JsonParser.getValueAsString(user, "password");
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public Properties getCredentials(Properties arg0, DistributedMember arg1, boolean arg2) throws AuthenticationFailedException {
        if (this.userName.isEmpty()) initialize();

        Properties properties = new Properties();
        properties.put(USER_NAME, this.userName);
        properties.put(PASSWORD , this.password);
        return properties;
    }
}
