START TRANSACTION;

    ALTER TABLE dpia
        ADD COLUMN from_external_source BIT DEFAULT 0 NOT NULL,
        ADD COLUMN external_link VARCHAR(2048) NULL;

    UPDATE dpia d
    JOIN dpia_screening s
    ON d.asset_id = s.asset_id
    SET d.external_link = s.consequence_link,
        d.from_external_source = 1
    WHERE s.consequence_link IS NOT NULL;

    ALTER TABLE dpia_screening
    DROP COLUMN consequence_link;

COMMIT;