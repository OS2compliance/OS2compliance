package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DPIAAnswerPlaceholder {
    DATA_PROCESSING_PERSONAL_DATA_WHO("{personoplysninger_hvem}", "Hvem har adgang til personoplysningerne"),
    DATA_PROCESSING_PERSONAL_DATA_HOW_MANY("{personoplysninger_hvor_mange}", "Hvor mange har adgang til personoplysningerne"),
    DATA_PROCESSING_PERSONAL_DATA_TYPES("{personoplysninger_typer}", "Typer af personoplysninger"),
    DATA_PROCESSING_PERSONAL_DATA_TYPES_FREETEXT("{personoplysninger_typer_fritekst}", "Typer af personoplysninger fritekst"),
    DATA_PROCESSING_PERSONAL_CATEGORIES_OF_REGISTERED("{kategorier_af_registrerede}", "Kategorier af registrerede"),
    DATA_PROCESSING_PERSONAL_HOW_LONG("{personoplysninger_hvor_længe}", "Hvor længe opbevares personoplysningerne"),
    DATA_PROCESSING_DELETE_LINK("{slettelink}", "Link til sletteprocedure");

    private final String placeholder;
    private final String description;

    private DPIAAnswerPlaceholder(String placeholder, String description) {
        this.placeholder = placeholder;
        this.description = description;
    }

}
