-- INSERT the control result choice list
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('control-result', 'Resultat af kontrol', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption, editable) VALUES
('control-result-no-error-123456', 'Ingen fejl konstateret', false),
('control-result-no-critical-error-123456', 'Ingen kritiske fejl konstateret', false),
('control-result-critical-error-123456', 'Kritiske fejl konstateret', false);

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
    'control-result-no-error-123456',
    'control-result-no-critical-error-123456',
    'control-result-critical-error-123456'
);

-- MIGRATE task_log TABLE
-- Add the new bigint column temporarily
ALTER TABLE task_logs ADD COLUMN new_result BIGINT;

-- Update new column with corresponding IDs from choice_values based on enum values
UPDATE task_logs t
    LEFT JOIN choice_values cv ON
        CASE t.task_result
            WHEN 'NO_ERROR' THEN cv.identifier = 'control-result-no-error-123456'
            WHEN 'NO_CRITICAL_ERROR' THEN cv.identifier = 'control-result-no-critical-error-123456'
            WHEN 'CRITICAL_ERROR' THEN cv.identifier = 'control-result-critical-error-123456'
            END
SET t.new_result = cv.id
WHERE t.task_result IS NOT NULL;

-- Drop the old column
ALTER TABLE task_logs DROP COLUMN task_result;

-- Rename the new column task_result
ALTER TABLE task_logs CHANGE COLUMN new_result task_result BIGINT NULL;

-- Add foreign key constraint
ALTER TABLE task_logs ADD CONSTRAINT fk_task_logs_result FOREIGN KEY (task_result) REFERENCES choice_values(id);