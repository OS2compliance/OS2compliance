CREATE TABLE register_custom_responsible_user_mapping (
    register_id BIGINT(20) NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (register_id, user_uuid),
    CONSTRAINT fk_register_custom_responsible_user_mapping_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_custom_responsible_user_mapping_user_uuid FOREIGN KEY (user_uuid) REFERENCES users (uuid) ON DELETE CASCADE
)  collate = utf8mb4_danish_ci;;