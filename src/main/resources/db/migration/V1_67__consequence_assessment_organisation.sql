-- INSERT THE EXISTING COLUMNS
-- Insert into choice_lists
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('organisation-assessment-columns', 'Organisations-vurderingskolonner', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption) VALUES
('organisation-rep', 'Vurdering for organisationen (Omdømme/tillid)'),
('organisation-eco', 'Vurdering for organisationen (Økonomi/jura/drift)');

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
    'organisation-rep',
    'organisation-eco'
);

-- Create the new organisation_assessment_columns table
CREATE TABLE organisation_assessment_columns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    choice_value_id BIGINT NOT NULL,
    availability INT,
    integrity INT,
    confidentiality INT,
    consequence_assessment_id BIGINT NOT NULL,

    -- Foreign key constraints
    CONSTRAINT fk_org_assessment_choice_value
        FOREIGN KEY (choice_value_id) REFERENCES choice_values(id),
    CONSTRAINT fk_org_assessment_consequence
        FOREIGN KEY (consequence_assessment_id) REFERENCES consequence_assessments(register_id)
);

-- Migration of existing data
INSERT INTO organisation_assessment_columns
    (choice_value_id, availability, integrity, confidentiality, consequence_assessment_id)
SELECT
    cv_rep.id as choice_value_id,
    ca.availability_organisation_rep as availability,
    ca.integrity_organisation_rep as integrity,
    ca.confidentiality_organisation_rep as confidentiality,
    ca.register_id as consequence_assessment_id
FROM consequence_assessments ca
CROSS JOIN choice_values cv_rep
WHERE cv_rep.identifier = 'organisation-rep'
  AND (ca.availability_organisation_rep IS NOT NULL
       OR ca.integrity_organisation_rep IS NOT NULL
       OR ca.confidentiality_organisation_rep IS NOT NULL);

INSERT INTO organisation_assessment_columns
    (choice_value_id, availability, integrity, confidentiality, consequence_assessment_id)
SELECT
    cv_eco.id as choice_value_id,
    ca.availability_organisation_eco as availability,
    ca.integrity_organisation_eco as integrity,
    ca.confidentiality_organisation_eco as confidentiality,
    ca.register_id as consequence_assessment_id
FROM consequence_assessments ca
CROSS JOIN choice_values cv_eco
WHERE cv_eco.identifier = 'organisation-eco'
  AND (ca.availability_organisation_eco IS NOT NULL
       OR ca.integrity_organisation_eco IS NOT NULL
       OR ca.confidentiality_organisation_eco IS NOT NULL);

-- Drop the old columns
ALTER TABLE consequence_assessments
DROP COLUMN availability_organisation_rep,
DROP COLUMN availability_organisation_eco,
DROP COLUMN integrity_organisation_rep,
DROP COLUMN integrity_organisation_eco,
DROP COLUMN confidentiality_organisation_rep,
DROP COLUMN confidentiality_organisation_eco;

-- Add new authenticity column
ALTER TABLE consequence_assessments ADD authenticity_society INT NULL;

-- Rename columns
ALTER TABLE consequence_assessments CHANGE confidentiality_reason registered_reason VARCHAR(255);
ALTER TABLE consequence_assessments CHANGE integrity_reason organisation_reason VARCHAR(255);
ALTER TABLE consequence_assessments CHANGE availability_reason society_reason VARCHAR(255);

-- Add inherited_authenticity_society to threat_assessments
ALTER TABLE threat_assessments ADD inherited_authenticity_society INT NULL;