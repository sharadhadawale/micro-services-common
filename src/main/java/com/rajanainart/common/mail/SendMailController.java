package com.rajanainart.common.mail;

import com.rajanainart.common.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;

@RestController
@RequestMapping("/mail")
public class SendMailController {
	private static final Logger log = LoggerFactory.getLogger(SendMailController.class);
	@Autowired
	private MailService service;
	
	/**Method is used to send mail
	 * @param mail
	 */
	@PostMapping("/send")
	public String sendEmail(@RequestBody Mail mail) {
		String status = null;
		log.info("Inside Mail service");
		try {
			service.sendEmail(mail);
			status = BaseRestController.SUCCESS;
			log.info("Mail sent successfully");
		} catch (MessagingException e) {
			status = String.format("%s: %s", BaseRestController.ERROR, e.getMessage());
			log.error("Error inside mail service"+ status);
		}
		return status;
	}
}
