
CREATE TABLE incidents
(
    id              BIGINT       NOT NULL,
    version         INT          NOT NULL,
    relation_type   VARCHAR(30)  NOT NULL,
    name            VARCHAR(768) NOT NULL,
    created_at      datetime(6)  NOT NULL,
    created_by      VARCHAR(255) NOT NULL,
    updated_at      datetime     NULL,
    updated_by      VARCHAR(255) NULL,
    deleted         BIT(1)       NULL,
    localized_enums VARCHAR(255) NULL,
    CONSTRAINT pk_incidents PRIMARY KEY (id)
) collate = utf8mb4_danish_ci;

CREATE TABLE incident_field_responses
(
    id                 BIGINT AUTO_INCREMENT NOT NULL,
    question           VARCHAR(2048)         NULL,
    incident_type      VARCHAR(255)          NULL,
    sort_key           BIGINT                NULL,
    defined_list       TEXT                  NULL,
    answer_text        TEXT                  NULL,
    answer_date_time   datetime              NULL,
    answer_element_ids TEXT                  NULL,
    incident_id        BIGINT                NULL,
    CONSTRAINT pk_incident_field_responses PRIMARY KEY (id)
) collate = utf8mb4_danish_ci;

ALTER TABLE incident_field_responses
    ADD CONSTRAINT FK_INCIDENT_FIELD_RESPONSES_ON_INCIDENT FOREIGN KEY (incident_id) REFERENCES incidents (id);

CREATE TABLE incident_fields
(
    id            BIGINT AUTO_INCREMENT NOT NULL,
    incident_type VARCHAR(255)          NULL,
    question      VARCHAR(2048)         NULL,
    sort_key      BIGINT                NULL,
    index_column  BIT(1)                NULL,
    defined_list  TEXT                  NULL,
    CONSTRAINT pk_incident_fields PRIMARY KEY (id)
) collate = utf8mb4_danish_ci;
