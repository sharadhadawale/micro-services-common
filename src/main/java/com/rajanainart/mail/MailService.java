package com.rajanainart.mail;

import java.util.Iterator;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.ByteArrayDataSource;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class MailService {

	private static final Logger log = LoggerFactory.getLogger(MailService.class);

	public static final String NO_REPLY = "noreply";

	@Autowired
	private JavaMailSender sender;

	/**
	 * Uses Java mail API and SMTP details to send mail to specified mail id
	 *
	 * @param mail
	 * @throws @throws Exception
	 */
	public void sendEmail(Mail mail) throws MessagingException {
		MimeMessage message = sender.createMimeMessage();
		log.info("Inside sendMail service");
		try {
			MimeMessageHelper helper = new MimeMessageHelper(message, true);
			helper.setTo(mail.getMailTo().stream().toArray(String[]::new));
			if (mail.getMailCc() != null)
				helper.setCc(mail.getMailCc().stream().toArray(String[]::new));

			helper.setSubject(mail.getMailSubject());
			helper.setText(mail.getMailBody(), mail.isHtml());
			helper.setFrom(mail.getMailFrom());
			if (mail.getAttachments() != null) {
				Iterator<Map.Entry<String, byte[]>> itr = mail.getAttachments().entrySet().iterator();
				while (itr.hasNext()) {
					Map.Entry<String, byte[]> entry = itr.next();
					String fileName = FilenameUtils.getExtension(entry.getKey());
					String mimeType = MimeTypeConstant.getMimeType(fileName);
					ByteArrayDataSource dataSource = new ByteArrayDataSource(entry.getValue(), mimeType);
					helper.addAttachment(entry.getKey(), dataSource);
				}
			}
			sender.send(message);
		}
		catch (MessagingException e) {
			log.error("Failure inside sendMail service");
			throw new MessagingException("Error in mail service" + e.getMessage());
		}
	}
}
