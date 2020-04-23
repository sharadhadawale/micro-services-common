package com.rajanainart;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.rajanainart.integration.IntegrationController;

@SpringBootApplication(scanBasePackages = { "com.rajanainart" })
public class ServicesApp {
    private static final Logger logger = LogManager.getLogger(ServicesApp.class);

    public static void main(String[] args) {
        SpringApplication.run(ServicesApp.class, args);
        try {
            IntegrationController integration = new IntegrationController();
            integration.startAutoIntegrations();
        }
        catch(Exception ex) {
            logger.error("Error while starting auto integrations");
            ex.printStackTrace();
        }
    }
}

