package dk.digitalidentity.config.property;


import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Kitos {
    private boolean enabled = true;
    private String email;
    private String password;
    private String basePath;
}
