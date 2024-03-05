CREATE TABLE assets_responsible_users_mapping
(
    asset_id BIGINT NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (asset_id, user_uuid),
    CONSTRAINT fk_assets_responsible_users_asset_id
        FOREIGN KEY (asset_id) REFERENCES assets (id),
    CONSTRAINT fk_assets_responsible_users_users_uuid
        FOREIGN KEY (user_uuid) REFERENCES users (uuid)
) COLLATE = utf8mb4_danish_ci;

INSERT INTO assets_responsible_users_mapping (asset_id, user_uuid)
SELECT id, responsible_uuid
FROM assets
WHERE responsible_uuid IS NOT NULL;

ALTER TABLE assets DROP COLUMN responsible_uuid;

CREATE TABLE registers_responsible_users_mapping
(
    register_id BIGINT NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (register_id, user_uuid),
    CONSTRAINT fk_registers_responsible_users_register_id
        FOREIGN KEY (register_id) REFERENCES registers (id),
    CONSTRAINT fk_registers_responsible_users_user_uuid
        FOREIGN KEY (user_uuid) REFERENCES users (uuid)
) COLLATE = utf8mb4_danish_ci;

INSERT INTO registers_responsible_users_mapping (register_id, user_uuid)
SELECT id, responsible_uuid
FROM registers
WHERE responsible_uuid IS NOT NULL;

ALTER TABLE registers DROP COLUMN responsible_uuid;

CREATE TABLE registers_responsible_ous_mapping
(
    register_id BIGINT NOT NULL,
    ou_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (register_id, ou_uuid),
    CONSTRAINT fk_registers_responsible_ous_register_id
        FOREIGN KEY (register_id) REFERENCES registers (id),
    CONSTRAINT fk_registers_responsible_ous_ou_uuid
        FOREIGN KEY (ou_uuid) REFERENCES ous (uuid)
) COLLATE = utf8mb4_danish_ci;

INSERT INTO registers_responsible_ous_mapping (register_id, ou_uuid)
SELECT id, responsible_ou_uuid
FROM registers
WHERE responsible_ou_uuid IS NOT NULL;

ALTER TABLE registers DROP COLUMN responsible_ou_uuid;

create table registers_departments_mapping
(
    register_id BIGINT NOT NULL,
    ou_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (register_id, ou_uuid),
    CONSTRAINT fk_registers_departments_register_id
        FOREIGN KEY (register_id) REFERENCES registers (id),
    CONSTRAINT fk_registers_departments_ou_uuid
        FOREIGN KEY (ou_uuid) REFERENCES ous (uuid)
) COLLATE = utf8mb4_danish_ci;

INSERT INTO registers_departments_mapping (register_id, ou_uuid)
SELECT id, department
FROM registers
WHERE department IS NOT NULL;

ALTER TABLE registers DROP COLUMN department;