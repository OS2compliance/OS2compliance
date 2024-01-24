
ALTER TABLE threat_assessment_responses RENAME threat_assessment_responses_old;

CREATE TABLE threat_assessment_responses
(
    id                           BIGINT       NOT NULL,
    version                      INT          NOT NULL,
    relation_type                VARCHAR(255) NOT NULL,
    name                         VARCHAR(768) NOT NULL,
    created_at                   datetime     NOT NULL,
    created_by                   VARCHAR(255) NOT NULL,
    updated_at                   datetime     NULL,
    updated_by                   VARCHAR(255) NULL,
    deleted                      BIT(1)       NULL,
    localized_enums              VARCHAR(255) NULL,
    not_relevant                 BIT(1)       NULL,
    probability                  INT          NULL,
    confidentiality_registered   INT          NULL,
    confidentiality_organisation INT          NULL,
    integrity_registered         INT          NULL,
    integrity_organisation       INT          NULL,
    availability_registered      INT          NULL,
    availability_organisation    INT          NULL,
    problem                      VARCHAR(2048)NULL,
    existing_measures            VARCHAR(2048)NULL,
    method                       TEXT         NULL,
    elaboration                  VARCHAR(2048)NULL,
    residual_risk_probability    INT          NULL,
    residual_risk_consequence    INT          NULL,
    threat_assessment_id         BIGINT       NULL,
    threat_catalog_threat_id     VARCHAR(255) NULL,
    custom_threat_id             BIGINT       NULL,
    CONSTRAINT pk_threat_assessment_responses_n PRIMARY KEY (id),
    constraint fk_threat_assessment_responses_custom_threat_n
        foreign key (custom_threat_id) references custom_threats (id)
            on delete cascade,
    constraint fk_threat_assessment_responses_threat_assessment_n
        foreign key (threat_assessment_id) references threat_assessments (id)
            on delete cascade,
    constraint fk_threat_assessment_responses_threat_catalog_t_n
        foreign key (threat_catalog_threat_id) references threat_catalog_threats (identifier)
            on delete cascade
) collate = utf8mb4_danish_ci;
