-- INSERT THE REGISTER STATUS CHOICE LIST
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('register-status', 'Status', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption, editable) VALUES
('register-status-not-started-123456', 'Ikke startet', false),
('register-status-in-progress-123456', 'I gang', false),
('register-status-ready-123456', 'Klar', false);

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
    'register-status-not-started-123456',
    'register-status-in-progress-123456',
    'register-status-ready-123456'
);

-- MIGRATE REGISTERS TABLE
-- Add the new bigint column temporarily
ALTER TABLE registers ADD COLUMN new_status BIGINT;

-- Update new column with corresponding IDs from choice_values based on enum values
UPDATE registers r
    LEFT JOIN choice_values cv ON
        CASE r.status
            WHEN 'NOT_STARTED' THEN cv.identifier = 'register-status-not-started-123456'
            WHEN 'IN_PROGRESS' THEN cv.identifier = 'register-status-in-progress-123456'
            WHEN 'READY' THEN cv.identifier = 'register-status-ready-123456'
            END
SET r.new_status = cv.id
WHERE r.status IS NOT NULL;

-- Set default value for any NULL status (Default -> NOT_STARTED)
UPDATE registers
SET new_status = (SELECT id FROM choice_values WHERE identifier = 'register-status-not-started-123456' LIMIT 1)
WHERE new_status IS NULL;

-- Drop the old column
ALTER TABLE registers DROP COLUMN status;

-- Rename the new column to status and add NOT NULL constraint (since your table has a default)
ALTER TABLE registers CHANGE COLUMN new_status status BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE registers ADD CONSTRAINT fk_registers_status FOREIGN KEY (status) REFERENCES choice_values(id);