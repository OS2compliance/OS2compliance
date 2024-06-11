create table precautions
(
    id                bigint           not null primary key,
    created_at        datetime(6)      not null,
    created_by        varchar(255)     not null,
    name              varchar(255)     not null,
    updated_at        datetime(6)      null,
    updated_by        varchar(255)     null,
    relation_type     varchar(30)      not null,
    version           int              not null,
    deleted           bit default b'0' null,
    localized_enums   varchar(255)     null,
    description       text             null
) collate = utf8mb4_danish_ci;