ALTER TABLE dpia
    ADD COLUMN responsible_user_uuid VARCHAR(36) NULL;

ALTER TABLE dpia
    ADD COLUMN responsible_ou_uuid VARCHAR(36) NULL;

ALTER TABLE dpia_screening
    ADD COLUMN conclusion VARCHAR(30) NULL;