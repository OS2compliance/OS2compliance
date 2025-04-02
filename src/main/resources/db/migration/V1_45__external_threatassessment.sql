START TRANSACTION;

    ALTER TABLE threat_assessments
        ADD COLUMN from_external_source BIT DEFAULT 0 NOT NULL,
        ADD COLUMN external_link VARCHAR(2048) NULL;

COMMIT;