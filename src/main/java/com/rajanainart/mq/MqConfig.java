package com.rajanainart.mq;

import com.rajanainart.config.XmlConfig;
import com.rajanainart.helper.XmlNodeHelper;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

@Component("mq-config")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class MqConfig implements XmlConfig {
    public enum MqServerType { RABBITMQ, KAFKA  }
    public enum RabbitMqExchangeType { TOPIC }
    public enum AuthMode     { PLAIN, USER_AUTH }
    public enum MqUsedFor    { PUBLISH, CONSUME }

    private String id   = "";
    private String name = "";
    private int    port = 15672;
    private String host = "";
    private String userName = "";
    private String password;
    private String binding      = "";
    private String exchangeName = "";
    private String queueName    = "";
    private String topicName    = "";
    private String consumerId   = "";
    private int    duration     = 10;
    private String virtualHost  = "";
    private String kafkaProps, jaasConfig, kerberosConfig;

    private AuthMode     mode   = AuthMode.USER_AUTH;
    private MqUsedFor    fr     = MqUsedFor.PUBLISH;
    private MqServerType server = MqServerType.KAFKA;
    private RabbitMqExchangeType exchangeType = RabbitMqExchangeType.TOPIC;

    public String       getId  () { return id  ; }
    public String       getName() { return name; }
    public MqServerType getMqServerType() { return server; }
    public RabbitMqExchangeType getExchangeType() { return exchangeType; }
    public String    getHost      () { return host    ; }
    public String    getUserName  () { return userName; }
    public String    getPassword  () { return password; }
    public String    getBindingKey() { return binding ; }
    public String    getExchangeName() { return exchangeName; }
    public String    getQueueName   () { return queueName   ; }
    public String    getTopicName   () { return topicName   ; }
    public String    getClientId    () { return consumerId  ; }
    public String    getKafkaProps  () { return kafkaProps  ; }
    public String    getJaasConfig  () { return jaasConfig  ; }
    public String  getKerberosConfig() { return kerberosConfig; }
    public int       getDurationMns () { return duration    ; }
    public String    getVirtualHost () { return virtualHost ; }
    public AuthMode  getAuthMode    () { return mode; }
    public MqUsedFor getMqUsedFor   () { return fr  ; }
    public int       getPort        () { return port; }
    public List<String> getHostNames() {
        StringTokenizer tokenizer = new StringTokenizer(host, ",");
        List<String>    hosts     = new ArrayList<>();
        while (tokenizer.hasMoreTokens())
            hosts.add(tokenizer.nextToken());
        return hosts;
    }

    public synchronized void configure(Node node) {
        synchronized (this) {
            id   = XmlNodeHelper.getAttributeValue(node, "id"  );
            name = XmlNodeHelper.getAttributeValue(node, "name");

            if (id.isEmpty() || name.isEmpty())
                throw new NullPointerException("Attributes id & name are mandatory");

            Node n = XmlNodeHelper.getChildNode(node, "mq-type");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    server = XmlNodeHelper.getNodeValueAsEnum(MqServerType.class, n);
            }
            n = XmlNodeHelper.getChildNode(node, "used-for");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    fr = XmlNodeHelper.getNodeValueAsEnum(MqUsedFor.class, n);
            }
            n = XmlNodeHelper.getChildNode(node, "auth-mode");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    mode = XmlNodeHelper.getNodeValueAsEnum(AuthMode.class, n);
            }

            n = XmlNodeHelper.getChildNode(node, "exchange-name");
            if (n == null)
                throw new NullPointerException("Exchange Name is mandatory");
            exchangeName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "queue-name");
            if (n != null)
                queueName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "kafka-topic-name");
            if (n != null)
                topicName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "kafka-client-id");
            if (n != null)
                consumerId = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "kafka-poll-duration-mns");
            if (n != null)
                duration = XmlNodeHelper.getNodeValueAsInteger(n);

            n = XmlNodeHelper.getChildNode(node, "exchange-type");
            if (n != null) {
                String value = XmlNodeHelper.getNodeValue(n);
                if (!value.isEmpty())
                    exchangeType = XmlNodeHelper.getNodeValueAsEnum(RabbitMqExchangeType.class, n);
            }

            n = XmlNodeHelper.getChildNode(node, "host");
            if (n == null)
                throw new NullPointerException("Host Name is mandatory");
            host = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "port");
            if (n != null)
                port = XmlNodeHelper.getNodeValueAsInteger(n);

            n = XmlNodeHelper.getChildNode(node, "username");
            if (n == null)
                throw new NullPointerException("UserName is mandatory");
            userName = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "password");
            if (n == null)
                throw new NullPointerException("Password is mandatory");
            password = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "virtual-host");
            if (n != null)
                virtualHost = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "kafka-properties");
            if (n != null)
                kafkaProps = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "jaas-config-path");
            if (n != null)
                jaasConfig = XmlNodeHelper.getNodeValue(n);

            n = XmlNodeHelper.getChildNode(node, "kerberos-config-path");
            if (n != null)
                kerberosConfig = XmlNodeHelper.getNodeValue(n);
        }
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s", id, name, server.toString(), fr.toString());
    }
}
