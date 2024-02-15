CREATE TABLE threat_assessment_users
(
    threat_assessment_id BIGINT       NOT NULL,
    user_uuid            varchar(36) NOT NULL
) collate = utf8mb4_danish_ci;
ALTER TABLE threat_assessment_users
    ADD CONSTRAINT fk_thrassuse_on_threat_assessment FOREIGN KEY (threat_assessment_id) REFERENCES threat_assessments (id);

ALTER TABLE threat_assessment_users
    ADD CONSTRAINT fk_thrassuse_on_user FOREIGN KEY (user_uuid) REFERENCES users (uuid);
