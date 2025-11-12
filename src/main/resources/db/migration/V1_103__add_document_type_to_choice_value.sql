-- INSERT the document type choice list
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('document-type', 'Dokumenttype', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption, editable) VALUES
('document-type-other-123456', 'Andet', false),
('document-type-workflow-123456', 'Arbejdsgang', false),
('document-type-data-processing-agreement-123456', 'Databehandleraftale', false),
('document-type-contract-123456', 'Kontrakt', false),
('document-type-control-123456', 'Kontrol', false),
('document-type-management-report-123456', 'Ledelsesrapport', false),
('document-type-procedure-123456', 'Procedure', false),
('document-type-risk-assessment-report-123456', 'Risikovurderingsrapport', false),
('document-type-supervisory-report-123456', 'Tilsynsrapport', false),
('document-type-guide-123456', 'Vejledning', false);

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
    'document-type-other-123456',
    'document-type-workflow-123456',
    'document-type-data-processing-agreement-123456',
    'document-type-contract-123456',
    'document-type-control-123456',
    'document-type-management-report-123456',
    'document-type-procedure-123456',
    'document-type-risk-assessment-report-123456',
    'document-type-supervisory-report-123456',
    'document-type-guide-123456'
);

-- MIGRATE documents TABLE
-- Add the new bigint column temporarily
ALTER TABLE documents ADD COLUMN new_document_type BIGINT;

-- Update new column with corresponding IDs from choice_values based on enum values
UPDATE documents d
    LEFT JOIN choice_values cv ON
        CASE d.document_type
            WHEN 'OTHER' THEN cv.identifier = 'document-type-other-123456'
            WHEN 'WORKFLOW' THEN cv.identifier = 'document-type-workflow-123456'
            WHEN 'DATA_PROCESSING_AGREEMENT' THEN cv.identifier = 'document-type-data-processing-agreement-123456'
            WHEN 'CONTRACT' THEN cv.identifier = 'document-type-contract-123456'
            WHEN 'CONTROL' THEN cv.identifier = 'document-type-control-123456'
            WHEN 'MANAGEMENT_REPORT' THEN cv.identifier = 'document-type-management-report-123456'
            WHEN 'PROCEDURE' THEN cv.identifier = 'document-type-procedure-123456'
            WHEN 'RISK_ASSESSMENT_REPORT' THEN cv.identifier = 'document-type-risk-assessment-report-123456'
            WHEN 'SUPERVISORY_REPORT' THEN cv.identifier = 'document-type-supervisory-report-123456'
            WHEN 'GUIDE' THEN cv.identifier = 'document-type-guide-123456'
            END
SET d.new_document_type = cv.id
WHERE d.document_type IS NOT NULL;

-- Drop the old column
ALTER TABLE documents DROP COLUMN document_type;

-- Rename the new column to document_type
ALTER TABLE documents CHANGE COLUMN new_document_type document_type BIGINT NULL;

-- Add foreign key constraint
ALTER TABLE documents ADD CONSTRAINT fk_documents_type FOREIGN KEY (document_type) REFERENCES choice_values(id);