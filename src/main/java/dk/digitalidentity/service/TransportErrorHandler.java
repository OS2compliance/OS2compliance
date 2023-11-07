package dk.digitalidentity.service;

import jakarta.mail.event.TransportEvent;
import jakarta.mail.event.TransportListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransportErrorHandler implements TransportListener {
	@Override
	public void messageDelivered(final TransportEvent e) {
		; // do nothing
	}
	
	@Override
	public void messageNotDelivered(final TransportEvent e) {
		log.warn("Message NOT delivered!");
	}
	
	@Override
	public void messagePartiallyDelivered(final TransportEvent e) {
		log.warn("Message partialy delivered!");
	}
	
}
