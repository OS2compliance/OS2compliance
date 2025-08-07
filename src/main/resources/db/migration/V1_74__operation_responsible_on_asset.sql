CREATE TABLE assets_operation_responsible_users_mapping
(
    asset_id BIGINT NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (asset_id, user_uuid),
    CONSTRAINT fk_assets_operation_responsible_users_asset_id
        FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_assets_operation_responsible_users_users_uuid
        FOREIGN KEY (user_uuid) REFERENCES users (uuid)
) COLLATE = utf8mb4_danish_ci;