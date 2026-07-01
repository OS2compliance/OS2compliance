CREATE TABLE auditlog (
    id                      BIGINT AUTO_INCREMENT PRIMARY KEY,
    created_timestamp       DATETIME,
    performer_uuid          VARCHAR(36),
    performer_name          VARCHAR(255),
    entity_id               BIGINT,
    entity_type             VARCHAR(255),
    entity_name             VARCHAR(255)
);
