package com.rajanainart.common.mq;

import java.io.Closeable;

public interface MqMessageHandler extends Closeable, Cloneable {
    void    init   (MqConfig config);
    boolean publish(String encodeType, String messageId, Object message);
    void    consume(MqOnMessageArrival callback);

    @FunctionalInterface
    interface MqOnMessageArrival {
        void process(Object result);
    }
}
