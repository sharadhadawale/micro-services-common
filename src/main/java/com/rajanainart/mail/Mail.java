package com.rajanainart.mail;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rajanainart.helper.MiscHelper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@JsonIgnoreProperties(ignoreUnknown=false)
public class Mail {
	
	private List<String> mailTo  ;
	private String 		 mailFrom;
	private List<String> mailCc  ;
	private String 		 mailSubject;
	private String 		 mailBody;
	private String 		 appName ;
	private boolean      html = false;
	private  Map<String,byte[]> attachments;

	public void setAttachments(Map<String, MultipartFile> files) {
		attachments = new HashMap<>();
		for (Map.Entry<String, MultipartFile> file : files.entrySet()) {
			try {
				attachments.put(file.getValue().getOriginalFilename(), file.getValue().getBytes());
			}
			catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	public static Mail getInstance(MailConfig config) {
		Mail mail = new Mail();
		mail.mailFrom    = config.getFrom   ();
		mail.mailTo      = config.getToList ();
		mail.mailSubject = config.getSubject();
		mail.mailBody    = config.getBody   ();
		mail.html		 = config.isHtml    ();

		return mail;
	}

	public static Mail getInstance(Map<String, Object> map) {
		Mail mail = new Mail();
		mail.mailFrom    = String.valueOf(map.get("mailFrom"));
		mail.mailTo      = Arrays.asList(String.valueOf(map.get("mailTo")).split(","));
		mail.mailSubject = String.valueOf(map.get("mailSubject"));
		mail.mailBody    = String.valueOf(map.get("mailBody"));
		mail.html		 = MiscHelper.convertStringToBoolean(String.valueOf(map.get("html")), false);

		return mail;
	}
}
