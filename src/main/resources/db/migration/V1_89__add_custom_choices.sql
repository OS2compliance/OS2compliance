-- INSERT THE SUPERVISION MODEL CHOICE LIST
-- Insert into choice_lists
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('supervision-model', 'Tilsynsmodel', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption) VALUES
                                                    ('supervision-model-selfcontrol-123456', 'Egenkontrol'),
                                                    ('supervision-model-physical-supervision-123456', 'Fysisk tilsyn'),
                                                    ('supervision-model-isae-3000-123456', 'ISAE 3000'),
                                                    ('supervision-model-isae-3402-123456', 'ISAE 3402'),
                                                    ('supervision-model-isrs-4400-123456', 'ISRS 4400'),
                                                    ('supervision-model-supervision-justified-suspicion-123456', 'Tilsyn udelukkende i tilfælde af begrundet mistanke'),
                                                    ('supervision-model-management-statement-123456', 'Ledelseserklæring'),
                                                    ('supervision-model-written-control-123456', 'Skriftlig kontrol'),
                                                    ('supervision-model-supervision-form-declaration-123456', 'Tilsynsskema med tro- og love erklæring'),
                                                    ('supervision-model-sworn-statement-123456', 'Tro- og love erklæring'),
                                                    ('supervision-model-independent-audit-123456', 'Uafhængig revisionserklæring uden typeangivelse'),
                                                    ('supervision-model-soc-statement-123456', 'SOC-erklæring'),
                                                    ('supervision-model-dsd-123456', 'DSD'),
                                                    ('supervision-model-dbs-123456', 'DBS');

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
                     'supervision-model-selfcontrol-123456',
                     'supervision-model-physical-supervision-123456',
                     'supervision-model-isae-3000-123456',
                     'supervision-model-isae-3402-123456',
                     'supervision-model-isrs-4400-123456',
                     'supervision-model-supervision-justified-suspicion-123456',
                     'supervision-model-management-statement-123456',
                     'supervision-model-written-control-123456',
                     'supervision-model-supervision-form-declaration-123456',
                     'supervision-model-sworn-statement-123456',
                     'supervision-model-independent-audit-123456',
                     'supervision-model-soc-statement-123456',
                     'supervision-model-dsd-123456',
                     'supervision-model-dbs-123456'
    );

-- CHANGE COLUMN TYPE FOR SUPERVISORY_MODEL IN ASSETS
-- Add the new bigint column temporarily
ALTER TABLE assets ADD COLUMN new_supervisory_model BIGINT;

-- Update new column with corresponding IDs from choice_values based on enum values
UPDATE assets a
    LEFT JOIN choice_values cv ON
        CASE a.supervisory_model
            WHEN 'SELFCONTROL' THEN cv.identifier = 'supervision-model-selfcontrol-123456'
            WHEN 'PHYSICAL_SUPERVISION' THEN cv.identifier = 'supervision-model-physical-supervision-123456'
            WHEN 'ISAE_3000' THEN cv.identifier = 'supervision-model-isae-3000-123456'
            WHEN 'ISAE_3402' THEN cv.identifier = 'supervision-model-isae-3402-123456'
            WHEN 'ISRS_4400' THEN cv.identifier = 'supervision-model-isrs-4400-123456'
            WHEN 'SUPERVISION_JUSTIFIED_SUSPICION' THEN cv.identifier = 'supervision-model-supervision-justified-suspicion-123456'
            WHEN 'MANAGEMENT_STATEMENT' THEN cv.identifier = 'supervision-model-management-statement-123456'
            WHEN 'WRITTEN_CONTROL' THEN cv.identifier = 'supervision-model-written-control-123456'
            WHEN 'SUPERVISION_FORM_DECLARATION_OF_FAITH_AND_LAWS' THEN cv.identifier = 'supervision-model-supervision-form-declaration-123456'
            WHEN 'SWORN_STATEMENT' THEN cv.identifier = 'supervision-model-sworn-statement-123456'
            WHEN 'INDEPENDENT_AUDIT' THEN cv.identifier = 'supervision-model-independent-audit-123456'
            WHEN 'SOC_STATEMENT' THEN cv.identifier = 'supervision-model-soc-statement-123456'
            WHEN 'DSD' THEN cv.identifier = 'supervision-model-dsd-123456'
            WHEN 'DBS' THEN cv.identifier = 'supervision-model-dbs-123456'
            END
SET a.new_supervisory_model = cv.id
WHERE a.supervisory_model IS NOT NULL;

-- Drop the old column
ALTER TABLE assets DROP COLUMN supervisory_model;

-- Rename the new column to supervisory_model (nullable since it can be null)
ALTER TABLE assets CHANGE COLUMN new_supervisory_model supervisory_model BIGINT NULL;

-- Add foreign key constraint
ALTER TABLE assets ADD CONSTRAINT fk_assets_supervisory_model
    FOREIGN KEY (supervisory_model) REFERENCES choice_values(id);


-- CHANGE COLUMN TYPE FOR SUPERVISION_MODEL IN ASSETS_OVERSIGHT
-- Add the new bigint column temporarily
ALTER TABLE assets_oversight ADD COLUMN new_supervision_model BIGINT;

-- Update new column with corresponding IDs from choice_values based on enum values
UPDATE assets_oversight ao
    LEFT JOIN choice_values cv ON
        CASE ao.supervision_model
            WHEN 'SELFCONTROL' THEN cv.identifier = 'supervision-model-selfcontrol-123456'
            WHEN 'PHYSICAL_SUPERVISION' THEN cv.identifier = 'supervision-model-physical-supervision-123456'
            WHEN 'ISAE_3000' THEN cv.identifier = 'supervision-model-isae-3000-123456'
            WHEN 'ISAE_3402' THEN cv.identifier = 'supervision-model-isae-3402-123456'
            WHEN 'ISRS_4400' THEN cv.identifier = 'supervision-model-isrs-4400-123456'
            WHEN 'SUPERVISION_JUSTIFIED_SUSPICION' THEN cv.identifier = 'supervision-model-supervision-justified-suspicion-123456'
            WHEN 'MANAGEMENT_STATEMENT' THEN cv.identifier = 'supervision-model-management-statement-123456'
            WHEN 'WRITTEN_CONTROL' THEN cv.identifier = 'supervision-model-written-control-123456'
            WHEN 'SUPERVISION_FORM_DECLARATION_OF_FAITH_AND_LAWS' THEN cv.identifier = 'supervision-model-supervision-form-declaration-123456'
            WHEN 'SWORN_STATEMENT' THEN cv.identifier = 'supervision-model-sworn-statement-123456'
            WHEN 'INDEPENDENT_AUDIT' THEN cv.identifier = 'supervision-model-independent-audit-123456'
            WHEN 'SOC_STATEMENT' THEN cv.identifier = 'supervision-model-soc-statement-123456'
            WHEN 'DSD' THEN cv.identifier = 'supervision-model-dsd-123456'
            WHEN 'DBS' THEN cv.identifier = 'supervision-model-dbs-123456'
            END
SET ao.new_supervision_model = cv.id
WHERE ao.supervision_model IS NOT NULL;

-- Drop the old column
ALTER TABLE assets_oversight DROP COLUMN supervision_model;

-- Rename the new column to supervision_model (nullable since it can be null)
ALTER TABLE assets_oversight CHANGE COLUMN new_supervision_model supervision_model BIGINT NULL;

-- Add foreign key constraint
ALTER TABLE assets_oversight ADD CONSTRAINT fk_assets_oversight_supervision_model
    FOREIGN KEY (supervision_model) REFERENCES choice_values(id);