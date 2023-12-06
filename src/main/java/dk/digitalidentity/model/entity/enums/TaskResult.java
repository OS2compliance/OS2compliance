package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum TaskResult {
    NO_ERROR("Ingen fejl konstateret"),
    NO_CRITICAL_ERROR("Ingen kritiske fejl konstateret"),
    CRITICAL_ERROR("Kritiske fejl konstateret");

    private final String value;

    TaskResult(String val) {this.value = val;}


}
