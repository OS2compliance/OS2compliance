package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    OTHER("Andet"),
    WORKFLOW("Arbejdsgang"),
    DATA_PROCESSING_AGREEMENT("Databehandleraftale"),
    CONTRACT("Kontrakt"),
    CONTROL("Kontrol"),
    MANAGEMENT_REPORT("Ledelsesrapport"),
    PROCEDURE("Procedure"),
    RISK_ASSESSMENT_REPORT("Risikovurderingsrapport"),
    SUPERVISORY_REPORT("Tilsynsrapport"),
    GUIDE("Vejledning");

    private final String message;

    DocumentType(final String message) {
        this.message = message;
    }

}
