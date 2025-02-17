-- ADD CUSTOMIZABLE COLUMN
ALTER TABLE choice_lists
ADD COLUMN customizable BIT(1) NOT NULL DEFAULT b'0';

UPDATE choice_lists
SET customizable = b'0'
WHERE customizable IS NULL;

-- INSERT THE EXISTING ASSET TYPES
-- Insert into choice_lists
INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('asset-type', 'Type af aktiv', 0, 1);

-- Get the ID of the inserted row
SET @choice_list_id = LAST_INSERT_ID();

-- Insert into choice_values
INSERT INTO choice_values (identifier, caption) VALUES
('asset-type-it-system-123456', 'It-system'),
('asset-type-module-123456', 'Module'),
('asset-type-server-123456', 'Server'),
('asset-type-service-123456', 'Service');

-- Insert into choice_list_values
INSERT INTO choice_list_values (choice_list_id, choice_value_id)
SELECT @choice_list_id, id FROM choice_values
WHERE identifier IN (
    'asset-type-it-system-123456',
    'asset-type-module-123456',
    'asset-type-server-123456',
    'asset-type-service-123456'
);

-- CHANGE COLUMN TYPE FOR ASSET
-- Add the new bigint column temporarily
ALTER TABLE assets ADD COLUMN new_asset_type BIGINT;

-- Update new column with corresponding IDs from choice_values
UPDATE assets a
LEFT JOIN choice_values cv ON LOWER(a.asset_type) = LOWER(cv.caption)
SET a.new_asset_type = cv.id;

-- Assign default value where no match was found
UPDATE assets
SET new_asset_type = (SELECT id FROM choice_values WHERE LOWER(caption) = 'it-system' LIMIT 1)
WHERE new_asset_type IS NULL;

-- Drop the old column
ALTER TABLE assets DROP COLUMN asset_type;

-- Rename the new column to asset_type and add NOT NULL constraint
ALTER TABLE assets CHANGE COLUMN new_asset_type asset_type BIGINT NOT NULL;

-- Add foreign key constraint
ALTER TABLE assets ADD CONSTRAINT fk_assets_asset_type
FOREIGN KEY (asset_type) REFERENCES choice_values(id);
