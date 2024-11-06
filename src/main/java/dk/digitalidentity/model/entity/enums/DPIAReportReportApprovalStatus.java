package dk.digitalidentity.model.entity.enums;

import lombok.Getter;

@Getter
public enum DPIAReportReportApprovalStatus {
    WAITING("Afventer"),
    SIGNED("Signeret");

    private final String message;

    DPIAReportReportApprovalStatus(final String message) {
        this.message = message;
    }
}
