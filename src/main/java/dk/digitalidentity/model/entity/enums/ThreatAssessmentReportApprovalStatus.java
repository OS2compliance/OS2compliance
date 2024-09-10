package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum ThreatAssessmentReportApprovalStatus {
    NOT_SENT("Ikke sendt"),
    WAITING("Afventer"),
    SIGNED("Signeret");

    private final String message;

    ThreatAssessmentReportApprovalStatus(final String message) {
        this.message = message;
    }
}
