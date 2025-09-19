CREATE TABLE threat_assessment_catalogs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    threat_assessment_id BIGINT NOT NULL,
    threat_catalog_identifier VARCHAR(255) NOT NULL,
    UNIQUE KEY unique_assessment_catalog (threat_assessment_id, threat_catalog_identifier),
    CONSTRAINT fk_threat_assessment_catalogs_assessment FOREIGN KEY (threat_assessment_id) REFERENCES threat_assessments(id) ON DELETE CASCADE,
    CONSTRAINT fk_threat_assessment_catalogs_catalog FOREIGN KEY (threat_catalog_identifier) REFERENCES threat_catalogs(identifier) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;

INSERT INTO threat_assessment_catalogs (threat_assessment_id, threat_catalog_identifier)
SELECT id, threat_catalog_identifier
FROM threat_assessments
WHERE threat_catalog_identifier IS NOT NULL
  AND deleted = false;

ALTER TABLE threat_assessments DROP FOREIGN KEY FK_THREAT_ASSESSMENTS_ON_THREAT_CATALOG_IDENTIFIER;
ALTER TABLE threat_assessments DROP COLUMN threat_catalog_identifier;