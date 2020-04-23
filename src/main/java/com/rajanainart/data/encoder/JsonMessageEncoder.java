package com.rajanainart.data.encoder;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.sql.ResultSet;

@Component("encode-json")
public class JsonMessageEncoder implements MessageEncoder {
    public  static final String NAME = "json";
    private static final Logger log  = LoggerFactory.getLogger(JsonMessageEncoder.class);

    public String buildMessage(Object message) {
        String result = "";
        try {
            ObjectMapper mapper = new ObjectMapper();
            result = mapper.writeValueAsString(message);
        }
        catch(IOException ex) {
            log.error("Error while encoding in json format");
            ex.printStackTrace();
        }
        return result;
    }

    public String buildMessage(ResultSet record) {
        return "Not Implemented";
    }
}
