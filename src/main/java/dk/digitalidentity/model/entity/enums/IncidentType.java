package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum IncidentType {
    TEXT("Tekst"),
    DATE_TIME("Dato/tid"),
    ASSET("Aktiv"),
    USER("Bruger"),
    USERS("Brguere"),
    ORGANIZATION("Enhed"),
    ORGANIZATIONS("Enheder"),
    DEFINED_LIST("Valgliste");

    private final String value;

    IncidentType(String value) {
        this.value = value;
    }
}
