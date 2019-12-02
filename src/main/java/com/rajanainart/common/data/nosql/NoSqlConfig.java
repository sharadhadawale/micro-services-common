package com.rajanainart.common.data.nosql;

import com.rajanainart.common.config.XmlConfig;
import com.rajanainart.common.helper.XmlNodeHelper;
import com.rajanainart.common.integration.iaas.IaaSRequest;
import org.apache.http.HttpHost;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component("nosql-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class NoSqlConfig implements XmlConfig {
    public enum NoSqlSourceType { ELASTICSEARCH }

    private String id, name;
    private String userName = "", password = "", surl = "";
    private NoSqlSourceType type = NoSqlSourceType.ELASTICSEARCH;

    public String getId      () { return id      ; }
    public String getName    () { return name    ; }
    public String getRawUrls () { return surl    ; }
    public String getUserName() { return userName; }
    public String getPassword() { return password; }

    public NoSqlSourceType getType() { return type; }

    public List<URL> getUrls() {
        List<URL> urls = new ArrayList<>();

        for (String url : surl.split(",")) {
            try {
                urls.add(new URL(url));
            }
            catch (MalformedURLException ex) {
                ex.printStackTrace();
            }
        }

        return urls;
    }

    public HttpHost[] getHttpHosts() {
        List<URL>  urls  = getUrls();
        HttpHost[] hosts = new HttpHost[urls.size()];

        for (URL url : urls)
            hosts[urls.indexOf(url)] = new HttpHost(url.getHost(), url.getPort(), url.getProtocol());
        return hosts;
    }

    public static NoSqlConfig getInstance(IaaSRequest iaaSRequest) {
        NoSqlConfig config = new NoSqlConfig();
        config.id   = iaaSRequest.getNoSqlConfigName();
        config.name = iaaSRequest.getNoSqlConfigName();
        String key  = iaaSRequest.getNoSqlConfig().getType().toUpperCase(Locale.ENGLISH);
        config.type = Enum.valueOf(NoSqlSourceType.class, key);
        config.surl = iaaSRequest.getNoSqlConfig().getUrl();

        return config;
    }

    @Override
    public synchronized void configure(Node node) {
        synchronized (this) {
            id    = XmlNodeHelper.getAttributeValue(node, "id"  );
            name  = XmlNodeHelper.getAttributeValue(node, "name");
            if (id.isEmpty())
                throw new NullPointerException("Attribute id is mandatory");

            Node n = XmlNodeHelper.getChildNode(node, "type");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    type = XmlNodeHelper.getNodeValueAsEnum(NoSqlSourceType.class, n);
            }

            n = XmlNodeHelper.getChildNode(node, "url");
            if (n != null)
                surl = XmlNodeHelper.getNodeValue(n);
            if (surl.isEmpty())
                throw new NullPointerException("Node url is mandatory");

            n = XmlNodeHelper.getChildNode(node, "username");
            if (n != null) userName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "password");
            if (n != null) password = XmlNodeHelper.getNodeValue(n);
        }
    }
}
