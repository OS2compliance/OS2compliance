-- Hibernate Envers audit schema: one revinfo table + one _aud table per @Audited
-- entity/collection. All columns in _aud tables are nullable regardless of the
-- source table's nullability, per Envers convention.

CREATE TABLE revinfo
(
    rev      INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    revtstmp BIGINT
);

-- ==================== assets ====================
CREATE TABLE assets_aud
(
    id                                BIGINT       NOT NULL,
    rev                               INT          NOT NULL,
    revtype                           TINYINT,
    version                           INT,
    relation_type                     VARCHAR(30),
    name                              VARCHAR(768),
    created_at                        DATETIME(6),
    created_by                        VARCHAR(255),
    updated_at                        DATETIME(6),
    updated_by                        VARCHAR(255),
    deleted                           BIT,
    localized_enums                   VARCHAR(255),
    description                       TEXT,
    ai_status                         VARCHAR(30),
    ai_risk                           VARCHAR(30),
    asset_category                    VARCHAR(30),
    asset_status                      VARCHAR(30),
    socially_critical                 TINYINT(1),
    archive                           VARCHAR(64),
    emergency_plan_link               VARCHAR(2048),
    re_establishment_plan_link        VARCHAR(2048),
    contract_link                     VARCHAR(2048),
    contract_date                     DATE,
    contract_termination              DATE,
    termination_notice                VARCHAR(1024),
    dpia_opt_out                      BIT,
    dpia_opt_out_reason               TEXT,
    active                            BOOLEAN,
    threat_assessment_opt_out         BIT,
    threat_assessment_opt_out_reason  TEXT,
    tia_opt_out                       BIT,
    tia_opt_out_reason                TEXT,
    asset_measure_status              VARCHAR(50),
    asset_type                        BIGINT,
    supervisory_model                 BIGINT,
    data_processing_id                BIGINT,
    oversight_responsible_uuid        VARCHAR(36),
    supplier_id                       BIGINT,
    criticality                       VARCHAR(30),
    data_processing_agreement_status  VARCHAR(30),
    data_processing_agreement_date    DATE,
    data_processing_agreement_link    VARCHAR(2048),
    next_inspection                   VARCHAR(255),
    next_inspection_date              DATE,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_assets_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE assets_responsible_users_mapping_aud
(
    asset_id  BIGINT      NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    rev       INT         NOT NULL,
    revtype   TINYINT,
    PRIMARY KEY (asset_id, user_uuid, rev),
    CONSTRAINT fk_assets_responsible_users_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE assets_users_mapping_aud
(
    asset_id  BIGINT      NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    rev       INT         NOT NULL,
    revtype   TINYINT,
    PRIMARY KEY (asset_id, user_uuid, rev),
    CONSTRAINT fk_assets_users_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE assets_operation_responsible_users_mapping_aud
(
    asset_id  BIGINT      NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    rev       INT         NOT NULL,
    revtype   TINYINT,
    PRIMARY KEY (asset_id, user_uuid, rev),
    CONSTRAINT fk_assets_operation_responsible_users_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE assets_additional_supervisory_models_aud
(
    asset_id        BIGINT NOT NULL,
    choice_value_id BIGINT NOT NULL,
    rev             INT    NOT NULL,
    revtype         TINYINT,
    PRIMARY KEY (asset_id, choice_value_id, rev),
    CONSTRAINT fk_assets_additional_supervisory_models_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE assets_departments_mapping_aud
(
    asset_id BIGINT      NOT NULL,
    ou_uuid  VARCHAR(36) NOT NULL,
    rev      INT         NOT NULL,
    revtype  TINYINT,
    PRIMARY KEY (asset_id, ou_uuid, rev),
    CONSTRAINT fk_assets_departments_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE asset_tag_aud
(
    asset_id BIGINT NOT NULL,
    tag_id   BIGINT NOT NULL,
    rev      INT    NOT NULL,
    revtype  TINYINT,
    PRIMARY KEY (asset_id, tag_id, rev),
    CONSTRAINT fk_asset_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== threat_assessments ====================
CREATE TABLE threat_assessments_aud
(
    id                                        BIGINT      NOT NULL,
    rev                                       INT         NOT NULL,
    revtype                                   TINYINT,
    version                                   INT,
    relation_type                             VARCHAR(30),
    name                                      VARCHAR(255),
    created_at                                DATETIME(6),
    created_by                                VARCHAR(255),
    updated_at                                DATETIME(6),
    updated_by                                VARCHAR(255),
    deleted                                   BIT,
    localized_enums                           VARCHAR(255),
    threat_assessment_type                    VARCHAR(255),
    responsible_uuid                          VARCHAR(36),
    responsible_ou_uuid                       VARCHAR(36),
    threat_assessment_report_s3_document_id   BIGINT,
    threat_assessment_report_user_uuid        VARCHAR(36),
    next_revision                             DATETIME(6),
    revision_interval                         VARCHAR(100),
    registered                                BIT,
    organisation                              BIT,
    society                                   BIT(1),
    authenticity                              BIT(1),
    inherit                                   BIT,
    inherited_confidentiality_registered      INT,
    inherited_confidentiality_organisation    INT,
    inherited_confidentiality_society         INT,
    inherited_integrity_registered            INT,
    inherited_integrity_organisation          INT,
    inherited_integrity_society               INT,
    inherited_availability_registered         INT,
    inherited_availability_organisation       INT,
    inherited_availability_society            INT,
    inherited_authenticity_society            INT,
    assessment                                VARCHAR(255),
    threat_assessment_report_approval_status  VARCHAR(255),
    from_external_source                      BIT,
    external_link                             VARCHAR(2048),
    comment                                   TEXT,
    hidden                                    BOOLEAN,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_threat_assessments_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE threat_assessment_users_aud
(
    threat_assessment_id BIGINT      NOT NULL,
    user_uuid             VARCHAR(36) NOT NULL,
    rev                   INT         NOT NULL,
    revtype               TINYINT,
    PRIMARY KEY (threat_assessment_id, user_uuid, rev),
    CONSTRAINT fk_threat_assessment_users_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE threat_assessment_catalogs_aud
(
    threat_assessment_id       BIGINT       NOT NULL,
    threat_catalog_identifier  VARCHAR(255) NOT NULL,
    rev                        INT          NOT NULL,
    revtype                    TINYINT,
    PRIMARY KEY (threat_assessment_id, threat_catalog_identifier, rev),
    CONSTRAINT fk_threat_assessment_catalogs_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE threat_assessment_tag_aud
(
    threat_assessment_id BIGINT NOT NULL,
    tag_id               BIGINT NOT NULL,
    rev                  INT    NOT NULL,
    revtype              TINYINT,
    PRIMARY KEY (threat_assessment_id, tag_id, rev),
    CONSTRAINT fk_threat_assessment_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== dpia ====================
CREATE TABLE dpia_aud
(
    id                                   BIGINT       NOT NULL,
    rev                                  INT          NOT NULL,
    revtype                              TINYINT,
    version                              INT,
    relation_type                        VARCHAR(30),
    name                                 VARCHAR(768),
    created_at                           DATETIME(6),
    created_by                           VARCHAR(255),
    updated_at                           DATETIME(6),
    updated_by                           VARCHAR(255),
    deleted                              BIT,
    localized_enums                      VARCHAR(255),
    dpia_checked_choice_list_identifiers TEXT,
    dpia_checked_threat_assessments_ids  TEXT,
    conclusion                           TEXT,
    next_revision                        DATETIME,
    revision_interval                    VARCHAR(255),
    comment                              VARCHAR(1024),
    from_external_source                 BIT,
    external_link                        VARCHAR(2048),
    user_updated_date                    DATE,
    responsible_user_uuid                VARCHAR(36),
    responsible_ou_uuid                  VARCHAR(36),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_dpia_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE dpia_asset_aud
(
    dpia_id  BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    rev      INT    NOT NULL,
    revtype  TINYINT,
    PRIMARY KEY (dpia_id, asset_id, rev),
    CONSTRAINT fk_dpia_asset_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE dpia_tag_aud
(
    dpia_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    rev     INT    NOT NULL,
    revtype TINYINT,
    PRIMARY KEY (dpia_id, tag_id, rev),
    CONSTRAINT fk_dpia_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== incidents ====================
CREATE TABLE incidents_aud
(
    id               BIGINT      NOT NULL,
    rev              INT         NOT NULL,
    revtype          TINYINT,
    version          INT,
    relation_type    VARCHAR(30),
    name             VARCHAR(768),
    created_at       DATETIME(6),
    created_by       VARCHAR(255),
    updated_at       DATETIME,
    updated_by       VARCHAR(255),
    deleted          BIT(1),
    localized_enums  VARCHAR(255),
    created_by_uuid  VARCHAR(16),
    draft            BOOLEAN,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_incidents_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== tasks ====================
CREATE TABLE tasks_aud
(
    id                          BIGINT       NOT NULL,
    rev                         INT          NOT NULL,
    revtype                     TINYINT,
    version                     INT,
    relation_type               VARCHAR(30),
    name                        VARCHAR(255),
    created_at                  DATETIME(6),
    created_by                  VARCHAR(255),
    updated_at                  DATETIME(6),
    updated_by                  VARCHAR(255),
    deleted                     BIT,
    localized_enums             VARCHAR(255),
    task_type                   VARCHAR(100),
    responsible_ou_uuid         VARCHAR(36),
    department_uuid             VARCHAR(36),
    next_deadline               DATE,
    repetition                  VARCHAR(100),
    description                 TEXT,
    notify_responsible          BIT,
    include_in_report           BOOLEAN,
    preserved_responsible_users VARCHAR(1000),
    task_description_template   BIGINT,
    notification_reminders      VARCHAR(255),
    in_progress                 BOOLEAN,
    in_progress_note            TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_tasks_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE task_responsible_users_aud
(
    task_id   BIGINT      NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    rev       INT         NOT NULL,
    revtype   TINYINT,
    PRIMARY KEY (task_id, user_uuid, rev),
    CONSTRAINT fk_task_responsible_users_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE task_tag_aud
(
    task_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    rev     INT    NOT NULL,
    revtype TINYINT,
    PRIMARY KEY (task_id, tag_id, rev),
    CONSTRAINT fk_task_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== registers ====================
CREATE TABLE registers_aud
(
    id                        BIGINT       NOT NULL,
    rev                       INT          NOT NULL,
    revtype                   TINYINT,
    version                   INT,
    relation_type             VARCHAR(30),
    name                      VARCHAR(768),
    created_at                DATETIME(6),
    created_by                VARCHAR(255),
    updated_at                DATETIME(6),
    updated_by                VARCHAR(255),
    deleted                   BIT,
    localized_enums           VARCHAR(255),
    criticality               VARCHAR(100),
    package_name              VARCHAR(50),
    description               TEXT,
    register_regarding        VARCHAR(255),
    security_precautions      TEXT,
    information_responsible   VARCHAR(255),
    purpose                   TEXT,
    purpose_notes             TEXT,
    emergency_plan_link       VARCHAR(2048),
    information_obligation_desc TEXT,
    consent                   TEXT,
    information_obligation    VARCHAR(100),
    status                    BIGINT,
    gdpr_choices              TEXT,
    supplemental_legal_basis  TEXT,
    data_processing_id        BIGINT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_registers_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE registers_responsible_users_mapping_aud
(
    register_id BIGINT      NOT NULL,
    user_uuid   VARCHAR(36) NOT NULL,
    rev         INT         NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (register_id, user_uuid, rev),
    CONSTRAINT fk_registers_responsible_users_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_custom_responsible_user_mapping_aud
(
    register_id BIGINT      NOT NULL,
    user_uuid   VARCHAR(36) NOT NULL,
    rev         INT         NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (register_id, user_uuid, rev),
    CONSTRAINT fk_register_custom_responsible_user_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE registers_responsible_ous_mapping_aud
(
    register_id BIGINT      NOT NULL,
    ou_uuid     VARCHAR(36) NOT NULL,
    rev         INT         NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (register_id, ou_uuid, rev),
    CONSTRAINT fk_registers_responsible_ous_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE registers_departments_mapping_aud
(
    register_id BIGINT      NOT NULL,
    ou_uuid     VARCHAR(36) NOT NULL,
    rev         INT         NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (register_id, ou_uuid, rev),
    CONSTRAINT fk_registers_departments_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_choice_value_registerregarding_mapping_aud
(
    register_id     BIGINT NOT NULL,
    choice_value_id BIGINT NOT NULL,
    rev             INT    NOT NULL,
    revtype         TINYINT,
    PRIMARY KEY (register_id, choice_value_id, rev),
    CONSTRAINT fk_register_choice_value_registerregarding_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_kle_main_group_aud
(
    register_id           BIGINT      NOT NULL,
    kle_main_group_number VARCHAR(50) NOT NULL,
    rev                   INT         NOT NULL,
    revtype               TINYINT,
    PRIMARY KEY (register_id, kle_main_group_number, rev),
    CONSTRAINT fk_register_kle_main_group_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_kle_group_aud
(
    register_id     BIGINT      NOT NULL,
    kle_group_number VARCHAR(50) NOT NULL,
    rev              INT         NOT NULL,
    revtype          TINYINT,
    PRIMARY KEY (register_id, kle_group_number, rev),
    CONSTRAINT fk_register_kle_group_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_kle_legal_reference_aud
(
    register_id      BIGINT      NOT NULL,
    accession_number VARCHAR(50) NOT NULL,
    rev              INT         NOT NULL,
    revtype          TINYINT,
    PRIMARY KEY (register_id, accession_number, rev),
    CONSTRAINT fk_register_kle_legal_reference_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_kle_subject_mapping_aud
(
    register_id    BIGINT      NOT NULL,
    subject_number VARCHAR(50) NOT NULL,
    rev            INT         NOT NULL,
    revtype        TINYINT,
    PRIMARY KEY (register_id, subject_number, rev),
    CONSTRAINT fk_register_kle_subject_mapping_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE register_tag_aud
(
    register_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    rev         INT    NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (register_id, tag_id, rev),
    CONSTRAINT fk_register_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== documents ====================
CREATE TABLE documents_aud
(
    id                    BIGINT       NOT NULL,
    rev                   INT          NOT NULL,
    revtype               TINYINT,
    version               INT,
    relation_type         VARCHAR(30),
    name                  VARCHAR(255),
    created_at            DATETIME(6),
    created_by            VARCHAR(255),
    updated_at            DATETIME(6),
    updated_by            VARCHAR(255),
    deleted               BIT,
    localized_enums       VARCHAR(255),
    responsible_uuid      VARCHAR(36),
    status                VARCHAR(100),
    document_type         BIGINT,
    description           TEXT,
    link                  VARCHAR(2048),
    document_version      VARCHAR(36),
    revision_interval     VARCHAR(100),
    next_revision         DATETIME(6),
    include_in_year_wheel BOOLEAN,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_documents_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE document_tag_aud
(
    document_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    rev         INT    NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (document_id, tag_id, rev),
    CONSTRAINT fk_document_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== standard_sections ====================
CREATE TABLE standard_sections_aud
(
    id                          BIGINT       NOT NULL,
    rev                         INT          NOT NULL,
    revtype                     TINYINT,
    version                     INT,
    relation_type               VARCHAR(30),
    name                        VARCHAR(768),
    created_at                  DATETIME(6),
    created_by                  VARCHAR(255),
    updated_at                  DATETIME(6),
    updated_by                  VARCHAR(255),
    deleted                     BIT,
    localized_enums             VARCHAR(255),
    template_section_identifier VARCHAR(255),
    description                 TEXT,
    reason                      TEXT,
    status                      VARCHAR(255),
    responsible_user_uuid       VARCHAR(36),
    selected                    TINYINT(1),
    nsis_practice               TEXT,
    nsis_smart                  TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_standard_sections_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== standard_templates ====================
CREATE TABLE standard_templates_aud
(
    identifier VARCHAR(255) NOT NULL,
    rev        INT          NOT NULL,
    revtype    TINYINT,
    name       VARCHAR(255),
    supporting TINYINT(1),
    PRIMARY KEY (identifier, rev),
    CONSTRAINT fk_standard_templates_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== standard_template_sections ====================
CREATE TABLE standard_template_sections_aud
(
    identifier                   VARCHAR(255) NOT NULL,
    rev                          INT          NOT NULL,
    revtype                      TINYINT,
    section                      VARCHAR(255),
    description                  TEXT,
    security_level               VARCHAR(128),
    sort_key                     BIGINT,
    parent_identifier            VARCHAR(255),
    standard_template_identifier VARCHAR(255),
    PRIMARY KEY (identifier, rev),
    CONSTRAINT fk_standard_template_sections_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== suppliers ====================
CREATE TABLE suppliers_aud
(
    id               BIGINT       NOT NULL,
    rev              INT          NOT NULL,
    revtype          TINYINT,
    version          BIGINT,
    relation_type    VARCHAR(30),
    name             VARCHAR(255),
    created_at       TIMESTAMP,
    created_by       VARCHAR(100),
    updated_at       TIMESTAMP,
    updated_by       VARCHAR(100),
    deleted          BIT,
    localized_enums  VARCHAR(255),
    responsible_uuid VARCHAR(36),
    status           VARCHAR(50),
    cvr              VARCHAR(10),
    zip              VARCHAR(10),
    city             VARCHAR(255),
    address          VARCHAR(255),
    contact          VARCHAR(255),
    phone            VARCHAR(50),
    email            VARCHAR(255),
    country          VARCHAR(255),
    personal_info    TINYINT(1),
    data_processor   TINYINT(1),
    description      TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_suppliers_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE supplier_tag_aud
(
    supplier_id BIGINT NOT NULL,
    tag_id      BIGINT NOT NULL,
    rev         INT    NOT NULL,
    revtype     TINYINT,
    PRIMARY KEY (supplier_id, tag_id, rev),
    CONSTRAINT fk_supplier_tag_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== users ====================
CREATE TABLE users_aud
(
    uuid     VARCHAR(36) NOT NULL,
    rev      INT         NOT NULL,
    revtype  TINYINT,
    user_id  VARCHAR(255),
    name     VARCHAR(255),
    email    VARCHAR(255),
    active   BIT,
    roles    VARCHAR(1024),
    password VARCHAR(255),
    PRIMARY KEY (uuid, rev),
    CONSTRAINT fk_users_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== choice_lists ====================
CREATE TABLE choice_lists_aud
(
    id           BIGINT       NOT NULL,
    rev          INT          NOT NULL,
    revtype      TINYINT,
    identifier   VARCHAR(255),
    name         VARCHAR(255),
    multi_select BIT,
    customizable BIT(1),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_choice_lists_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE choice_list_values_aud
(
    choice_list_id  BIGINT NOT NULL,
    choice_value_id BIGINT NOT NULL,
    rev             INT    NOT NULL,
    revtype         TINYINT,
    PRIMARY KEY (choice_list_id, choice_value_id, rev),
    CONSTRAINT fk_choice_list_values_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

-- ==================== email_templates ====================
CREATE TABLE email_templates_aud
(
    id            BIGINT       NOT NULL,
    rev           INT          NOT NULL,
    revtype       TINYINT,
    title         VARCHAR(255),
    message       TEXT,
    template_type VARCHAR(64),
    enabled       BIT(1),
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_email_templates_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);
