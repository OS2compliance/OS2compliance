package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum IncidentType {
    TEXT("Tekst"),
    DATE("Dato"),
    ASSET("Aktiv"),
    ASSETS("Aktiver"),
    USER("Bruger"),
    USERS("Brugere"),
    SUPPLIER("Leverandør"),
    SUPPLIERS("Leverandører"),
    ORGANIZATION("Enhed"),
    ORGANIZATIONS("Enheder"),
    CHOICE_LIST("Valgliste"),
    CHOICE_LIST_MULTIPLE("Valgliste (flere valg)");

    private final String value;

    IncidentType(String value) {
        this.value = value;
    }
}
