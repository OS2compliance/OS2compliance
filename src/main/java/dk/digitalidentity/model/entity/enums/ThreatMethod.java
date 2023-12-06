package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThreatMethod {
	NONE(""), ACCEPT("Accepter"), AVOID("Undgå"), SHARE("Del"), MITIGER("Mitiger");

    private String message;
    ThreatMethod(String message) {
        this.message = message;
    }
}
