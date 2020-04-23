package com.rajanainart.mail;

import com.rajanainart.helper.MiscHelper;
import com.rajanainart.rest.BaseRestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.MessagingException;
import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("/mail")
public class SendMailController {
	private static final Logger log = LoggerFactory.getLogger(SendMailController.class);
	@Autowired
	private MailService service;
	
	/**Method is used to send mail
	 * @param mail
	 */
	@PostMapping(value = "/send")
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

	@PostMapping(value = "/send-with-attachment")
	public String sendEmailWithAttachment(HttpServletRequest servletRequest, @RequestParam Map<String, Object> requestParams) {
		Map<String, MultipartFile> attachments = MiscHelper.getUploadedFiles(servletRequest);
		Mail   mail	  = Mail.getInstance(requestParams);
		String status = null;
		log.info("Inside Mail service");
		try {
			mail.setAttachments(attachments);
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
