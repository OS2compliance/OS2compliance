ALTER TABLE tags
    ADD COLUMN color_hex_code VARCHAR(7) NULL DEFAULT '#adb5bd';

UPDATE tags
SET color_hex_code = '#adb5bd'
WHERE color_hex_code IS NULL;

ALTER TABLE tags
    MODIFY COLUMN color_hex_code VARCHAR(7) NOT NULL DEFAULT '#adb5bd';