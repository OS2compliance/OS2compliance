-- Move screening relation from asset to dpia
ALTER TABLE dpia_screening
    DROP FOREIGN KEY FK_DPIA_ON_ASSET;

ALTER TABLE dpia_screening
    ADD COLUMN dpia_id BIGINT(20);

UPDATE dpia_screening ds
    LEFT JOIN dpia d ON ds.asset_id = d.asset_id
SET ds.dpia_id = d.id
WHERE d.id IS NOT NULL;

ALTER TABLE dpia_screening
    ADD CONSTRAINT fk_dpia_screening_dpia_id
        FOREIGN KEY (dpia_id) REFERENCES dpia (id);

ALTER TABLE dpia_screening
    DROP COLUMN asset_id;

-- remove any screening not connected to anything (empty)
DELETE
FROM dpia_screening
WHERE dpia_id IS NULL;

-- create many-to-many relation table between dpia and asset
CREATE TABLE dpia_asset
(
    id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    asset_id BIGINT NOT NULL,
    dpia_id  BIGINT NOT NULL,
    CONSTRAINT fk_dpia_asset_asset_id FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE CASCADE,
    CONSTRAINT fk_dpia_asset_dpia_id FOREIGN KEY (dpia_id) REFERENCES dpia (id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;

INSERT INTO dpia_asset (asset_id, dpia_id)
SELECT asset_id, id
FROM dpia;

ALTER TABLE dpia
    DROP FOREIGN KEY FK_ASSET_DPIA_ON_ASSET;

ALTER TABLE dpia
DROP COLUMN asset_id

UPDATE dpia d
SET d.name = (SELECT a.name
              FROM assets a
                       LEFT JOIN dpia_asset da on a.id = da.asset_id
              WHERE da.dpia_id = d.id
              LIMIT 1)
WHERE d.name = 'Konsekvensanalyse'