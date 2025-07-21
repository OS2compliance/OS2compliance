package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum RegisterSetting {
    CUSTOMRESPONSIBLEUSERFIELDNAME("custom_responsible_user_field_name");

    private final String value;

    RegisterSetting(String value) {this.value = value;}
}
