ALTER TABLE assets_oversight ADD COLUMN localized_enums varchar(255);
ALTER TABLE assets ADD COLUMN oversight_responsible_uuid varchar(36);