package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ChoiceOfSupervisionModel {

    SELFCONTROL("Egenkontrol"),
    PHYSICAL_SUPERVISION("Fysisk tilsyn"),
    ISAE_3000("ISAE 3000"),
    ISAE_3402("ISAE 3402"),
    ISRS_4400("ISRS 4400"),
    SUPERVISION_JUSTIFIED_SUSPICION("Tilsyn udelukkende i tilfælde af begrundet mistanke"),
    MANAGEMENT_STATEMENT("Ledelseserklæring"),
    WRITTEN_CONTROL("Skriftlig kontrol"),
    SUPERVISION_FORM_DECLARATION_OF_FAITH_AND_LAWS("Tilsynsskema med tro- og love erklæring"),
    SWORN_STATEMENT("Tro- og love erklæring"),
    INDEPENDENT_AUDIT("Uafhængig revisionserklæring uden typeangivelse"),
    SOC_STATEMENT("SOC-erklæring");

    private final String message;

    ChoiceOfSupervisionModel(final String message) {
        this.message = message;
    }
}
