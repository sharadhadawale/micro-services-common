package com.rajanainart.mq;

import com.rajanainart.config.AppContext;
import com.rajanainart.data.encoder.MessageEncoder;
import com.rajanainart.property.PropertyUtilExt;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.*;

@Component("kafka")
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class KafkaMessageHandler implements MqMessageHandler {
    private static final Logger logger = LoggerFactory.getLogger(KafkaMessageHandler.class);

    public final static Map<String, MessageEncoder> MESSAGE_ENCODERS = AppContext.getBeansOfType(MessageEncoder.class);

    private MqConfig   config;
    private Properties properties;
    private Consumer<String, String> consumer;
    private Producer<String, String> producer;

    public MqConfig getMqConfig() { return config; }

    @Override
    public void init(MqConfig config) {
        this.config = config;
        properties  = new Properties();

        if (!config.getKafkaProps().isEmpty()) {
            try (PropertyUtilExt p = new PropertyUtilExt(config.getKafkaProps())) {
                p.mergeProperties(properties);
            }
        }
        if (!config.getJaasConfig().isEmpty())
            System.setProperty("java.security.auth.login.config", config.getJaasConfig());
        if (!config.getKerberosConfig().isEmpty())
            System.setProperty("java.security.krb5.conf", config.getKerberosConfig());

        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getHost());
        if (config.getMqUsedFor() == MqConfig.MqUsedFor.CONSUME) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG                , config.getClientId());
            properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG  , StringDeserializer.class.getName());
            properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
            properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG        , "5");
            consumer = new KafkaConsumer<>(properties);
            List<String> topics = new ArrayList<>();
            topics.add(config.getTopicName());
            consumer.subscribe(topics);
        }
        else if (config.getMqUsedFor() == MqConfig.MqUsedFor.PUBLISH) {
            try {
                properties.put(ProducerConfig.CLIENT_ID_CONFIG             , config.getClientId());
                properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG  , StringSerializer.class.getName());
                properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
                producer = new KafkaProducer<>(properties);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public boolean publish(String encodeType, String messageId, Object message) {
        String         type    = String.format("encode-%s", encodeType);
        MessageEncoder encoder = KafkaMessageHandler.MESSAGE_ENCODERS.getOrDefault(type, null);
        if (encoder == null) {
            String log = String.format("Publish can't be completed, as the MessageEncoder is not available for %s", type);
            logger.error(log);
            return false;
        }
        String msg = encoder.buildMessage(message);
        ProducerRecord<String, String> record = new ProducerRecord<>(config.getTopicName(), messageId, msg);
        try {
            RecordMetadata metadata = producer.send(record).get();
            String log = String.format("Message has been successfully published. Kafka meta results, topic:%s, partition:%s, offset:%s, timestamp: %s",
                                        metadata.topic(), metadata.partition(), metadata.offset(), metadata.timestamp());
            logger.info(log);
            return true;
        }
        catch(Exception ex) {
            logger.error("Exception occurred while publishing kafka message");
            ex.printStackTrace();
            return false;
        }
    }

    @Override
    public void consume(MqOnMessageArrival callback) {
        while (true) {
            if (false) break; //sonar fix
            ConsumerRecords<String, String> consumerRecords = consumer.poll(1000);
            try {
                consumer.commitAsync();
            }
            catch (Exception ex) {
                logger.error("Error while committing offset");
                ex.printStackTrace();
            }
            if (callback != null)
                callback.process(consumerRecords);
        }
    }

    @Override
    public void close() {
        if (consumer != null) {
            logger.error("Closing consumer client");
            consumer.close();
        }
        if (producer != null) {
            logger.error("Closing producer client");
            producer.flush();
            producer.close();
        }
    }
}
