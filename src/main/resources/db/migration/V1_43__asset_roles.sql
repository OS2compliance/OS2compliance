create table role
(
    id          BIGINT auto_increment primary key,
    name        VARCHAR(255) NOT NULL
) collate = utf8mb4_danish_ci;;

create table asset_role
(
    id            bigint auto_increment primary key,
    asset_id      bigint not null,
        CONSTRAINT fk_assetrole_asset_id FOREIGN KEY (asset_id) references assets (id) ON DELETE CASCADE,
    role_id       bigint not null,
        CONSTRAINT fk_assetrole_role_id FOREIGN KEY (role_id) references role (id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;

create table role_assignment
(
    id            bigint auto_increment primary key,
    user_uuid      VARCHAR(36) not null,
        CONSTRAINT fk_roleassignment_user_uuid FOREIGN KEY (user_uuid) references users (uuid) ON DELETE CASCADE,
    asset_role_id       bigint not null,
        CONSTRAINT fk_roleassignment_asset_role_id FOREIGN KEY (asset_role_id) references asset_role (id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;