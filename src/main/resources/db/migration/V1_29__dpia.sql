ALTER TABLE data_processing ADD types_of_personal_information_freetext TEXT NULL;

ALTER TABLE assets ADD dpia_opt_out_reason TEXT NULL;

ALTER TABLE dpia RENAME dpia_screening;
ALTER TABLE dpia_screening DROP answer_a, DROP answer_b, DROP answer_c, DROP answer_d, DROP conclusion;

CREATE TABLE dpia (
   id BIGINT AUTO_INCREMENT NOT NULL,
   asset_id BIGINT NULL,
   dpia_checked_choice_list_identifiers TEXT NULL,
   dpia_checked_threat_assessments_ids TEXT NULL,
   conclusion TEXT NULL,
   next_revision datetime NULL,
   revision_interval VARCHAR(255) NULL,
   CONSTRAINT pk_dpia PRIMARY KEY (id)
);

ALTER TABLE dpia ADD CONSTRAINT FK_ASSET_DPIA_ON_ASSET FOREIGN KEY (asset_id) REFERENCES assets (id);

CREATE TABLE dpia_template_section (
   id BIGINT AUTO_INCREMENT NOT NULL,
   sort_key BIGINT NULL,
   identifier VARCHAR(255) NOT NULL,
   heading VARCHAR(1024) NOT NULL,
   explainer TEXT NULL,
   can_opt_out BOOLEAN NOT NULL DEFAULT FALSE,
   has_opted_out BOOLEAN NOT NULL DEFAULT FALSE,
   CONSTRAINT pk_dpia_template_section PRIMARY KEY (id)
);

CREATE TABLE dpia_template_question (
   id BIGINT AUTO_INCREMENT NOT NULL,
   dpia_template_section_id BIGINT NULL,
   sort_key BIGINT NULL,
   question TEXT NOT NULL,
   instructions TEXT NULL,
   answer_template TEXT NULL,
   deleted BOOLEAN NULL,
   CONSTRAINT pk_dpia_template_question PRIMARY KEY (id)
);

ALTER TABLE dpia_template_question ADD CONSTRAINT FK_DPIA_TEMPLATE_QUESTION_ON_DPIA_TEMPLATE_SECTION FOREIGN KEY (dpia_template_section_id) REFERENCES dpia_template_section (id);

CREATE TABLE dpia_response_section (
   id BIGINT AUTO_INCREMENT NOT NULL,
   dpia_id BIGINT NULL,
   dpia_template_section_id BIGINT NULL,
   selected BIT(1) NULL,
   CONSTRAINT pk_dpia_response_section PRIMARY KEY (id)
);

ALTER TABLE dpia_response_section ADD CONSTRAINT FK_DPIA_RESPONSE_SECTION_ON_DPIA FOREIGN KEY (dpia_id) REFERENCES dpia (id) ON DELETE CASCADE;
ALTER TABLE dpia_response_section ADD CONSTRAINT FK_DPIA_RESPONSE_SECTION_ON_DPIA_TEMPLATE_SECTION FOREIGN KEY (dpia_template_section_id) REFERENCES dpia_template_section (id);

CREATE TABLE dpia_response_section_answer (
   id BIGINT AUTO_INCREMENT NOT NULL,
   dpia_response_section_id BIGINT NULL,
   dpia_template_question_id BIGINT NULL,
   response TEXT NULL,
   CONSTRAINT pk_dpia_response_section_answer PRIMARY KEY (id)
);

ALTER TABLE dpia_response_section_answer ADD CONSTRAINT FK_DPIA_RESPONSE_SECTION_ANSWER_ON_DPIA_RESPONSE_SECTION FOREIGN KEY (dpia_response_section_id) REFERENCES dpia_response_section (id) ON DELETE CASCADE;
ALTER TABLE dpia_response_section_answer ADD CONSTRAINT FK_DPIA_RESPONSE_SECTION_ANSWER_ON_DPIA_TEMPLATE_QUESTION FOREIGN KEY (dpia_template_question_id) REFERENCES dpia_template_question (id);

CREATE TABLE dpia_report (
   id BIGINT AUTO_INCREMENT NOT NULL,
   dpia_id BIGINT NOT NULL,
   dpia_report_s3_document_id BIGINT NULL,
   report_approver_uuid VARCHAR(36) NOT NULL,
   report_approver_name VARCHAR(255) NOT NULL,
   dpia_report_approval_status VARCHAR(255) NOT NULL,
   CONSTRAINT pk_dpia_report PRIMARY KEY (id)
);

ALTER TABLE dpia_report ADD CONSTRAINT FK_DPIA_REPORT_ON_DPIA FOREIGN KEY (dpia_id) REFERENCES dpia (id);
ALTER TABLE dpia_report ADD CONSTRAINT FK_DPIA_REPORT_ON_DPIA_REPORT_S3_DOCUMENT FOREIGN KEY (dpia_report_s3_document_id) REFERENCES s3_document (id);