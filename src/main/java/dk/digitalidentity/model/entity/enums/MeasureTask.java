package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum MeasureTask {
    NEJ("Nej"), SKAL_AFKLARES("Skal afklares"), SKAL_GENERERE_EN_OPGAVE("Skal generere en opgave");

    private final String message;

    MeasureTask(String message) {
        this.message = message;
    }
}
