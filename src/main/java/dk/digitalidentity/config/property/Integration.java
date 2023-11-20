package dk.digitalidentity.config.property;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

@Getter
@Setter
public class Integration {
    @NestedConfigurationProperty
    private Cvr cvr = new Cvr();

    @NestedConfigurationProperty
    private OS2Sync os2Sync = new OS2Sync();

    @NestedConfigurationProperty
    private Kitos kitos = new Kitos();
}
