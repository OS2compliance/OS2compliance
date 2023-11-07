package dk.digitalidentity.service;

import dk.digitalidentity.config.OS2complianceConfiguration;
import jakarta.mail.Message;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
@Slf4j
public class MailService {
	@Autowired
	private OS2complianceConfiguration configuration;
	
	public boolean sendMessage(final String email, final String subject, final String message) {
		if (configuration.getMail().isEnabled()) {
			Transport transport = null;
			
			log.info("Sending email: '" + subject + "' to " + email);
			
			try {
				final Properties props = System.getProperties();
				props.put("mail.transport.protocol", "smtps");
				props.put("mail.smtp.port", 587);
				props.put("mail.smtp.auth", "true");
				props.put("mail.smtp.starttls.enable", "true");
				props.put("mail.smtp.starttls.required", "true");
				final Session session = Session.getDefaultInstance(props);
	
				
				final MimeMessage msg = new MimeMessage(session);
				msg.setFrom(new InternetAddress(configuration.getMail().getFrom(), configuration.getMail().getFromName()));
				
				for (final String singleEmail : email.split(";")) {
					final String trimmedEmail = singleEmail.trim();
					if (!StringUtils.isEmpty(trimmedEmail)) {
						msg.addRecipient(Message.RecipientType.TO, new InternetAddress(trimmedEmail));
					}
				}
				
				msg.setSubject(subject, "UTF-8");
				msg.setHeader("Content-Type", "text/html; charset=UTF-8");
				
				final MimeBodyPart messageBodyPart = new MimeBodyPart();
				messageBodyPart.setContent(message, "text/html; charset=UTF-8");
				
				final Multipart multipart = new MimeMultipart();
				multipart.addBodyPart(messageBodyPart);
				
				msg.setContent(multipart);
				
				transport = session.getTransport();
				transport.connect(configuration.getMail().getHost(), configuration.getMail().getUsername(), configuration.getMail().getPassword());
				transport.addTransportListener(new TransportErrorHandler());
				transport.sendMessage(msg, msg.getAllRecipients());
			} catch (final Exception ex) {
				log.error("Failed to send email", ex);
				
				return false;
			} finally {
				try {
					if (transport != null) {
						transport.close();
					}
				} catch (final Exception ex) {
					log.warn("Error occured while trying to terminate connection", ex);
				}
			}
			
			return true;
		}
		return false;
	}
	
	
}
