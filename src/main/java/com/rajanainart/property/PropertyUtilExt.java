package com.rajanainart.property;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Properties;

public class PropertyUtilExt implements Closeable {
    private static final Logger logger = LogManager.getLogger(PropertyUtilExt.class);

    private Reader     reader;
    private Properties props ;

    public Properties getProperties() { return props; }

    public PropertyUtilExt(String properties) {
        reader = new StringReader(properties);
        props  = new Properties();
        try {
            props.load(reader);
        }
        catch (IOException ex) {
            logger.error("Error while converting string to properties");
            ex.printStackTrace();
        }
    }

    public void mergeProperties(Properties properties) {
        for (String p : props.stringPropertyNames())
            properties.put(p, props.getProperty(p));
    }

    @Override
    public void close() {
        try {
            if (reader != null)
                reader.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
