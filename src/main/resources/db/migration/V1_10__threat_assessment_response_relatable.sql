ALTER TABLE threat_assessment_responses ADD COLUMN created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP;
ALTER TABLE threat_assessment_responses ADD COLUMN created_by VARCHAR(255) NOT NULL DEFAULT 'ukendt';
ALTER TABLE threat_assessment_responses ADD COLUMN updated_at DATETIME(6) NULL;
ALTER TABLE threat_assessment_responses ADD COLUMN updated_by VARCHAR(255) NULL;
ALTER TABLE threat_assessment_responses ADD COLUMN name VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE threat_assessment_responses ADD COLUMN relation_type VARCHAR(30) DEFAULT 'THREAT_ASSESSMENT_RESPONSE';
ALTER TABLE threat_assessment_responses ADD COLUMN version INT NOT NULL DEFAULT 0;
ALTER TABLE threat_assessment_responses ADD COLUMN deleted BIT NULL DEFAULT b'0';
ALTER TABLE threat_assessment_responses ADD COLUMN localized_enums VARCHAR(255) NULL;