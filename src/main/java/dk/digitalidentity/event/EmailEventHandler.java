package dk.digitalidentity.event;

import dk.digitalidentity.config.OS2complianceConfiguration;
import dk.digitalidentity.service.MailLogService;
import dk.digitalidentity.service.TransportErrorHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventHandler {
    private final OS2complianceConfiguration configuration;
	private final MailLogService mailLogService;

    @Async
    @EventListener
    // NOTICE: Attachments will be deleted after the event has been processed
    public void handleEmailEvent(final EmailEvent event) {
        if (!configuration.getMail().isEnabled()) {
            log.info("Email disabled NOT Sending email to " + event.getEmail());
            deleteAttachements(event);
            return;
        }
        Transport transport = null;
        log.info("Sending email: '{}' to {}", event.getSubject(), event.getEmail());
        if (event.getEmail() == null) {
            log.warn("Email is null. Skipping email event");
            return;
        }

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

            for (final String singleEmail : event.getEmail().split(";")) {
                final String trimmedEmail = singleEmail.trim();
                if (!StringUtils.isEmpty(trimmedEmail)) {
                    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(trimmedEmail));
                }
            }

            msg.setSubject(event.getSubject(), "UTF-8");
            msg.setHeader("Content-Type", "text/html; charset=UTF-8");

            final MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setContent(event.getMessage(), "text/html; charset=UTF-8");

            final Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageBodyPart);
            addAttachments(multipart, event.getAttachments());
            msg.setContent(multipart);

            transport = session.getTransport();
            transport.connect(configuration.getMail().getHost(), configuration.getMail().getUsername(), configuration.getMail().getPassword());
            transport.addTransportListener(new TransportErrorHandler());
            transport.sendMessage(msg, msg.getAllRecipients());
        } catch (final Exception ex) {
            log.error("Failed to send email", ex);
        } finally {
            deleteAttachements(event);
			mailLogService.logMail(event.getEmail(), event.getSubject(), event.getTemplateType());
            try {
                if (transport != null) {
                    transport.close();
                }
            } catch (final Exception ex) {
                log.warn("Error occured while trying to terminate connection", ex);
            }
        }
    }

    private static void deleteAttachements(final EmailEvent event) {
        //noinspection ResultOfMethodCallIgnored
        event.getAttachments().stream()
            .map(EmailEvent.EmailAttachement::inputFilePath)
            .map(File::new)
            .forEach(File::delete);
    }

    private void addAttachments(final Multipart multipart, final List<EmailEvent.EmailAttachement> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        for (final EmailEvent.EmailAttachement attachement : attachments) {
            try {
                final File f = new File(attachement.inputFilePath());
                final MimeBodyPart attachmentPart = new MimeBodyPart();
                attachmentPart.attachFile(f);
                attachmentPart.setFileName(attachement.wantedFilenameInEmail());
                multipart.addBodyPart(attachmentPart);
            } catch (final IOException | MessagingException e) {
                log.error("Failed to attach file", e);
            }
        }
    }

}
