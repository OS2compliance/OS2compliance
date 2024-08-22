package dk.digitalidentity;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication(scanBasePackages = "dk.digitalidentity")
public class Application {

	public static void main(String[] args) {
        Security.addProvider(new BouncyCastleProvider());

		SpringApplication.run(Application.class, args);
	}

}
