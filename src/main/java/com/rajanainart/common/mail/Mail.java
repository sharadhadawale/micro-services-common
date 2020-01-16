package com.rajanainart.common.mail;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown=false)
public class Mail {
	
	private List<String> mailTo  ;
	private String 		 mailFrom;
	private List<String> mailCc  ;
	private String 		 mailSubject;
	private String 		 mailBody;
	private String 		 appName ;
	private  Map<String,byte[]> attachments;

	public static Mail getInstance(MailConfig config) {
		Mail mail = new Mail();
		mail.mailFrom    = config.getFrom   ();
		mail.mailTo      = config.getToList ();
		mail.mailSubject = config.getSubject();
		mail.mailBody    = config.getBody   ();

		return mail;
	}
}
