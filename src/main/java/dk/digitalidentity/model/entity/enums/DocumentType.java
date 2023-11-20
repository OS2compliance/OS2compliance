package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DocumentType {
    PROCEDURE("Procedure"),
    GUIDE("Vejledning"),
    WORKFLOW("Arbejdsgang"),
    CONTRACT("Kontrakt"),
    DATA_PROCESSING_AGREEMENT("Databehandleraftale"),
    SUPERVISORY_REPORT("Tilsynsrapport"),
    MANAGEMENT_REPORT("Ledelsesrapport"),
    RISK_ASSESSMENT_REPORT("Risikovurderingsrapport"),
    OTHER("Andet");

    private final String message;

    DocumentType(final String message) {
        this.message = message;
    }

}
