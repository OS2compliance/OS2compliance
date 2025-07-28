package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KLEClient {
	private boolean enabled = true;
	private String allCron;
}
