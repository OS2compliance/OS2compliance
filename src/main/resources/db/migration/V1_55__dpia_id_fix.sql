START TRANSACTION;

alter table dbs_asset
    modify column id bigint not null;

ALTER TABLE dpia_response_section
    DROP FOREIGN KEY FK_DPIA_RESPONSE_SECTION_ON_DPIA;
ALTER TABLE dpia_report
    DROP FOREIGN KEY FK_DPIA_REPORT_ON_DPIA;
ALTER TABLE dpia_screening
    DROP FOREIGN KEY fk_dpia_screening_dpia_id;
ALTER TABLE dpia_asset
    DROP FOREIGN KEY fk_dpia_asset_dpia_id;

ALTER TABLE dpia
    MODIFY COLUMN id BIGINT NOT NULL;

-- Genskab FK (tilføj evt. ON DELETE/UPDATE som før)
ALTER TABLE dpia_response_section
    ADD CONSTRAINT FK_DPIA_RESPONSE_SECTION_ON_DPIA
        FOREIGN KEY (dpia_id) REFERENCES dpia (id) ON DELETE CASCADE;
ALTER TABLE dpia_report
    ADD CONSTRAINT FK_DPIA_REPORT_ON_DPIA
        FOREIGN KEY (dpia_id) REFERENCES dpia (id);

ALTER TABLE dpia_screening
    ADD CONSTRAINT fk_dpia_screening_dpia_id
        FOREIGN KEY (dpia_id) REFERENCES dpia (id);
ALTER TABLE dpia_asset
    ADD CONSTRAINT fk_dpia_asset_dpia_id
        FOREIGN KEY (dpia_id) REFERENCES dpia (id) ON DELETE CASCADE;

COMMIT;