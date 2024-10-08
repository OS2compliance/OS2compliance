package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum IncidentType {
    TEXT("Tekst"),
    DATE("Dato"),
    ASSET("Aktiv"),
    USER("Bruger"),
    USERS("Brugere"),
    ORGANIZATION("Enhed"),
    ORGANIZATIONS("Enheder"),
    CHOICE_LIST("Valgliste");

    private final String value;

    IncidentType(String value) {
        this.value = value;
    }
}
