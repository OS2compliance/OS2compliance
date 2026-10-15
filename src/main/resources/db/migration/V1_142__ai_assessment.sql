ALTER TABLE choice_values MODIFY COLUMN caption VARCHAR(1024) NOT NULL;

CREATE TABLE ai_assessment
(
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id       BIGINT NOT NULL,
    ai_actor_role  VARCHAR(50),
    ai_risk_factor VARCHAR(30),
    risk_note      TEXT,
    CONSTRAINT uk_ai_assessment_asset UNIQUE (asset_id),
    CONSTRAINT fk_ai_assessment_asset FOREIGN KEY (asset_id) REFERENCES assets (id)
) COLLATE = utf8mb4_danish_ci;

CREATE TABLE ai_assessment_answer
(
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    ai_assessment_id BIGINT      NOT NULL,
    choice_value_id  BIGINT      NOT NULL,
    answer           VARCHAR(30) NOT NULL,
    note             TEXT,
    CONSTRAINT uk_ai_assessment_answer_question UNIQUE (ai_assessment_id, choice_value_id),
    CONSTRAINT fk_ai_assessment_answer_assessment FOREIGN KEY (ai_assessment_id) REFERENCES ai_assessment (id),
    CONSTRAINT fk_ai_assessment_answer_question FOREIGN KEY (choice_value_id) REFERENCES choice_values (id)
) COLLATE = utf8mb4_danish_ci;

CREATE TABLE ai_assessment_aud
(
    id             BIGINT NOT NULL,
    rev            INT    NOT NULL,
    revtype        TINYINT,
    asset_id       BIGINT,
    ai_actor_role  VARCHAR(50),
    ai_risk_factor VARCHAR(30),
    risk_note      TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_ai_assessment_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

CREATE TABLE ai_assessment_answer_aud
(
    id               BIGINT NOT NULL,
    rev              INT    NOT NULL,
    revtype          TINYINT,
    ai_assessment_id BIGINT,
    choice_value_id  BIGINT,
    answer           VARCHAR(30),
    note             TEXT,
    PRIMARY KEY (id, rev),
    CONSTRAINT fk_ai_assessment_answer_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (rev)
);

INSERT INTO ai_assessment (asset_id, ai_risk_factor)
SELECT id, ai_risk
FROM assets
WHERE ai_risk IS NOT NULL;

ALTER TABLE assets DROP COLUMN ai_risk;
