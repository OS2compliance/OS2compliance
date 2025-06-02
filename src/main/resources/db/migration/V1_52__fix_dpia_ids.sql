-- Disable foreign key checks temporarily
SET FOREIGN_KEY_CHECKS = 0;


-- Add a temporary column to store old DPIA IDs
ALTER TABLE dpia ADD COLUMN temp_old_id BIGINT;
UPDATE dpia SET temp_old_id = id;

-- Retrieve the current max value from hibernate_sequences
SELECT next_val INTO @currentMax FROM hibernate_sequences WHERE sequence_name = 'default';


-- Start assigning new IDs to DPIA entities
UPDATE dpia SET id = (@currentMax := @currentMax + 1);

-- Update hibernate_sequences to reflect the new max value
UPDATE hibernate_sequences
SET next_val = @currentMax
WHERE sequence_name = 'default';

-- Update dpia_asset table to reference the new DPIA IDs
UPDATE dpia_asset
SET dpia_id = (
    SELECT d.id FROM dpia d WHERE d.temp_old_id = dpia_asset.dpia_id
);

-- Re-enable foreign key checks
SET FOREIGN_KEY_CHECKS = 1;
