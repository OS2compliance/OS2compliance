CREATE TABLE s3_document (
   id                  BIGINT AUTO_INCREMENT NOT NULL,
   s3file_key          VARCHAR(1024) NOT NULL,
   tts                 datetime NOT NULL,
   CONSTRAINT pk_s3_document PRIMARY KEY (id)
);

ALTER TABLE threat_assessments
   ADD COLUMN threat_assessment_report_s3_document_id BIGINT NULL,
   ADD COLUMN threat_assessment_report_user_uuid VARCHAR(36) NULL,
   ADD COLUMN threat_assessment_report_approval_status VARCHAR(255) NOT NULL DEFAULT 'NOT_SENT';
ALTER TABLE threat_assessments ADD CONSTRAINT fk_threat_assessment_s3_document FOREIGN KEY (threat_assessment_report_s3_document_id) REFERENCES s3_document (id);
ALTER TABLE threat_assessments ADD CONSTRAINT fk_threat_assessment_report_approver FOREIGN KEY (threat_assessment_report_user_uuid) REFERENCES users (uuid);