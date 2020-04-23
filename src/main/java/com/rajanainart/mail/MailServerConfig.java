package com.rajanainart.mail;

import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import com.rajanainart.property.PropertyUtil;

@Configuration
public class MailServerConfig {

	private static final Logger log = LoggerFactory.getLogger(MailServerConfig.class);

	@Value("${smtp.host}")
	private String host;
	@Value("${smtp.port}")
	private Integer port;
	@Value("${smtp.username}")
	private String userName;
	@Value("${smtp.password}")
	private String password;

	@Bean
	public JavaMailSender javaMailService() {
		JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
		log.info("the email host is " + host);
		log.info("the email port is " + port);
		javaMailSender.setHost(host);
		javaMailSender.setPort(port);
		javaMailSender.setUsername(userName);
		javaMailSender.setPassword(password);
		javaMailSender.setJavaMailProperties(getMailProperties());
		return javaMailSender;
	}

	private Properties getMailProperties() {
		String prefix = "smtp-property.";
		Properties properties = new Properties();
		for (Map.Entry<String, String> p : PropertyUtil.getAllProperties().entrySet()) {
			if (!p.getKey().startsWith(prefix))
				continue;

			String key = p.getKey().replace(prefix, "");
			String value = PropertyUtil.getPropertyValue(p.getKey(), "");
			properties.put(key, value);
		}
		return properties;
	}

}
