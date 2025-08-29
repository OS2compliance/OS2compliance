create table assets_departments_mapping
(
    asset_id BIGINT NOT NULL,
    ou_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (asset_id, ou_uuid),
    CONSTRAINT fk_assets_departments_asset_id
        FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_assets_departments_ou_uuid
        FOREIGN KEY (ou_uuid) REFERENCES ous (uuid)
) COLLATE = utf8mb4_danish_ci;