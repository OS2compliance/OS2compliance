package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
@Getter
@Setter
public class Mail {
	private boolean enabled;
	private String from = "no-reply@os2compliance.dk";
	private String fromName = "OS2Compliance deadline nearing";
	private String username;
	private String password;
	private String host;
}
