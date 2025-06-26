ALTER TABLE assets ADD COLUMN archive_temp VARCHAR(64);

UPDATE assets SET archive_temp = 'B' WHERE archive = true;
UPDATE assets SET archive_temp = 'UNDECIDED' WHERE archive = false;

ALTER TABLE assets DROP COLUMN archive;

ALTER TABLE assets RENAME COLUMN archive_temp TO archive;