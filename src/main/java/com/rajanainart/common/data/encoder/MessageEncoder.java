package com.rajanainart.common.data.encoder;

import java.sql.ResultSet;

public interface MessageEncoder {
    String buildMessage(Object    message);
    String buildMessage(ResultSet record );
}
