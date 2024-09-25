package dk.digitalidentity.model.entity;

import dk.digitalidentity.model.entity.enums.DPIAReportReportApprovalStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "dpia_report")
@Getter
@Setter
public class DPIAReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne
    @JoinColumn(name = "dpia_id")
    private DPIA dpia;

    @ManyToOne
    @JoinColumn(name = "dpia_report_s3_document_id")
    private S3Document dpiaReportS3Document;

    @Column
    @Enumerated(EnumType.STRING)
    private DPIAReportReportApprovalStatus dpiaReportApprovalStatus = DPIAReportReportApprovalStatus.WAITING;

    @Column
    private String reportApproverUuid;

    @Column
    private String reportApproverName;
}
