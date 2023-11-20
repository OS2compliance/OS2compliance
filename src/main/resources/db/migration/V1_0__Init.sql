create table SPRING_SESSION
(
    PRIMARY_ID            char(36)     not null primary key,
    SESSION_ID            char(36)     not null,
    CREATION_TIME         bigint       not null,
    LAST_ACCESS_TIME      bigint       not null,
    MAX_INACTIVE_INTERVAL int          not null,
    EXPIRY_TIME           bigint       not null,
    PRINCIPAL_NAME        varchar(255) null,
    constraint SESSION_ix1
        unique (SESSION_ID)
)
    row_format = DYNAMIC;

create index SESSION_ix2
    on SPRING_SESSION (EXPIRY_TIME);

create index SESSION_ix3
    on SPRING_SESSION (PRINCIPAL_NAME);

create table SPRING_SESSION_ATTRIBUTES
(
    SESSION_PRIMARY_ID char(36)     not null,
    ATTRIBUTE_NAME     varchar(200) not null,
    ATTRIBUTE_BYTES    blob         not null,
    primary key (SESSION_PRIMARY_ID, ATTRIBUTE_NAME),
    constraint SESSION_ATTRIBUTES_fk
        foreign key (SESSION_PRIMARY_ID) references SPRING_SESSION (PRIMARY_ID)
            on delete cascade
)
    row_format = DYNAMIC;

create table api_clients
(
    id                     bigint auto_increment primary key,
    name                   varchar(255) null,
    api_key                varchar(255) null,
    application_identifier varchar(255) null
);

create index index_api_clients_key
    on api_clients (api_key);

create table choice_lists
(
    id           bigint auto_increment primary key,
    identifier   varchar(255) not null,
    name         varchar(255) not null,
    multi_select bit          not null,
    constraint UC_ChoiceList_Identifier
        unique (identifier)
) collate = utf8mb4_danish_ci;

create index choice_lists_name__index
    on choice_lists (name);

create table choice_values
(
    id                    bigint auto_increment primary key,
    identifier            varchar(255) not null,
    caption               varchar(255) not null,
    description           text         null,
    limit_lower           int          null,
    limit_upper           int          null,
    child_list_identifier varchar(255) null,
    constraint UC_ChoiceValue_Identifier
        unique (identifier)
) collate = utf8mb4_danish_ci;

create table choice_list_values
(
    choice_list_id  bigint not null,
    choice_value_id bigint not null,
    primary key (choice_list_id, choice_value_id),
    constraint fk_lv_choice_list_id
        foreign key (choice_list_id) references choice_lists (id),
    constraint fk_lv_choice_value_id
        foreign key (choice_value_id) references choice_values (id)
) collate = utf8mb4_danish_ci;

create table choices_dpia
(
    id            bigint auto_increment primary key,
    identifier    varchar(255)  not null,
    category      varchar(255)  null,
    sub_category  varchar(255)  null,
    name          varchar(1024) not null,
    authorization varchar(255)  not null,
    multi_select  bit           not null,
    constraint UC_ChoiceDPIA_Identifier
        unique (identifier)
) collate = utf8mb4_danish_ci;

create table choice_dpia_values
(
    choice_dpia_id  bigint not null,
    choice_value_id bigint not null,
    primary key (choice_dpia_id, choice_value_id),
    constraint fk_dv_choice_dpia_id
        foreign key (choice_dpia_id) references choices_dpia (id),
    constraint fk_dv_choice_value_id
        foreign key (choice_value_id) references choice_values (id)
) collate = utf8mb4_danish_ci;

create table choices_measures
(
    id           bigint auto_increment primary key,
    identifier   varchar(255) not null,
    category     varchar(255) not null,
    name         varchar(255) not null,
    multi_select bit          not null,
    constraint UC_ChoiceMeasures_Identifier
        unique (identifier)
) collate = utf8mb4_danish_ci;

create table choice_measures_values
(
    choice_measure_id bigint not null,
    choice_value_id   bigint not null,
    primary key (choice_measure_id, choice_value_id),
    constraint fk_mv_choice_measure_id
        foreign key (choice_measure_id) references choices_measures (id),
    constraint fk_mv_choice_value_id
        foreign key (choice_value_id) references choice_values (id)
) collate = utf8mb4_danish_ci;

create table contacts
(
    id              bigint auto_increment primary key,
    version         bigint                                  not null,
    name            varchar(255)                            not null,
    created_at      timestamp default current_timestamp()   not null,
    created_by      varchar(100)                            not null,
    updated_at      timestamp                               not null,
    updated_by      varchar(100)                            null,
    relation_type   varchar(30)                             not null,
    role            varchar(255)                            null,
    phone           varchar(255)                            null,
    mail            varchar(255)                            null,
    deleted         bit       default b'0'                  null,
    localized_enums varchar(255)                            null
) collate = utf8mb4_danish_ci;

create index contacts_deleted__index
    on contacts (deleted);

create index contacts_name_index
    on contacts (name);

create table data_processing
(
    access_who_identifiers         varchar(1024) null,
    access_count_identifier        varchar(255)  null,
    person_count_identifier        varchar(255)  null,
    person_cat_info_identifiers_l1 varchar(1024) null,
    person_cat_info_identifiers_l2 varchar(1024) null,
    person_cat_reg_identifiers     varchar(1024) null,
    storage_time_identifier        varchar(255)  null,
    deletion_procedure             varchar(100)  null,
    deletion_procedure_link        varchar(1024) null,
    elaboration                    text          null,
    id                             bigint auto_increment
        primary key
) collate = utf8mb4_danish_ci;

create table assets
(
    id                               bigint                  not null primary key,
    created_at                       datetime(6)             not null,
    created_by                       varchar(255)            not null,
    name                             varchar(768)            not null,
    updated_at                       datetime(6)             null,
    updated_by                       varchar(255)            null,
    relation_type                    varchar(30)             not null,
    version                          int                     not null,
    responsible_uuid                 varchar(36)             null,
    product_link                     varchar(1024)           null,
    emergency_plan_link              varchar(1024)           null,
    re_establishment_plan_link       varchar(1024)           null,
    description                      text                    null,
    asset_type                       varchar(30)             not null,
    asset_status                     varchar(30)             not null,
    criticality                      varchar(30)             not null,
    data_processing_agreement_status varchar(30)             not null,
    supplier_id                      bigint                  null,
    socially_critical                tinyint(1) default 0    not null,
    archive                          tinyint(1) default 0    not null,
    contract_link                    varchar(1024)           null,
    contract_date                    date                    null,
    contract_termination             date                    null,
    termination_notice               varchar(1024)           null,
    supervisory_model                varchar(255)            null,
    next_inspection                  varchar(255)            null,
    next_inspection_date             date                    null,
    data_processing_agreement_date   date                    null,
    data_processing_agreement_link   varchar(1024)           null,
    data_processing_id               bigint                  null,
    deleted                          bit        default b'0' null,
    localized_enums                  varchar(255)            null,
    constraint fk_data_processing_id
        foreign key (data_processing_id) references data_processing (id)
) collate = utf8mb4_danish_ci;

create index assets_deleted__index
    on assets (deleted);

create index idx_assets_localized_enums
    on assets (localized_enums);

create index idx_assets_name
    on assets (name);

create table assets_measures
(
    id        bigint auto_increment primary key,
    asset_id  bigint        not null,
    choice_id bigint        not null,
    answer    varchar(255)  null,
    note      varchar(1024) null,
    task      varchar(255)  null,
    constraint fk_assets_measures_asset_id
        foreign key (asset_id) references assets (id),
    constraint fk_assets_measures_choice_id
        foreign key (choice_id) references choices_measures (id)
) collate = utf8mb4_danish_ci;

create table assets_oversight
(
    id                       bigint auto_increment primary key,
    asset_id                 bigint        not null,
    responsible_uuid         varchar(36)   not null,
    supervision_model        varchar(255)  not null,
    conclusion               varchar(1023) null,
    status                   varchar(20)   not null,
    creation_date            date          not null,
    next_inspection_deadline date          null,
    constraint fk_assets_oversight_asset_id
        foreign key (asset_id) references assets (id)
) collate = utf8mb4_danish_ci;

create table data_processing_categories_registered
(
    id                                      bigint auto_increment primary key,
    person_categories_registered_identifier varchar(200)  null,
    data_processing_id                      bigint        null,
    person_cat_info_identifiers             varchar(2048) null,
    information_receivers                   text          null,
    information_passed_on                   varchar(20)   null,
    constraint FK_DATA_PROCESSING_CATEGORIES_REGISTERED_ON_DATA_PROCESSING
        foreign key (data_processing_id) references data_processing (id)
);

create table documents
(
    id                bigint           not null primary key,
    created_at        datetime(6)      not null,
    created_by        varchar(255)     not null,
    name              varchar(255)     not null,
    updated_at        datetime(6)      null,
    updated_by        varchar(255)     null,
    relation_type     varchar(30)      not null,
    version           int              not null,
    description       varchar(255)     null,
    document_type     varchar(100)     null,
    link              varchar(255)     null,
    next_revision     datetime(6)      null,
    revision_interval varchar(100)     null,
    status            varchar(100)     null,
    responsible_uuid  varchar(36)      null,
    document_version  varchar(36)      null,
    deleted           bit default b'0' null,
    localized_enums   varchar(255)     null
) collate = utf8mb4_danish_ci;

create index documents_deleted__index
    on documents (deleted);

create index documents_name_index
    on documents (name);

create index idx_documents_localized_enums
    on documents (localized_enums);

create table dpia
(
    id               bigint auto_increment primary key,
    asset_id         bigint        null,
    answer_a         text          null,
    answer_b         text          null,
    answer_c         text          null,
    answer_d         text          null,
    conclusion       text          null,
    consequence_link varchar(2048) null,
    constraint FK_DPIA_ON_ASSET
        foreign key (asset_id) references assets (id)
);

create table dpia_screening_answers
(
    id            bigint auto_increment primary key,
    choice_id     bigint       not null,
    answer        varchar(255) null,
    assessment_id bigint       null,
    constraint fk_assets_dpia_choice_id
        foreign key (choice_id) references choices_dpia (id)
) collate = utf8mb4_danish_ci;

create table hibernate_sequences
(
    next_val      bigint       not null,
    sequence_name varchar(255) not null
        primary key
);

create table kitos_roles
(
    uuid        varchar(255) not null primary key,
    name        varchar(255) null,
    description text         null
) collate = utf8mb4_danish_ci;

create table ous
(
    uuid        varchar(255) not null primary key,
    active      bit          null,
    name        varchar(255) null,
    parent_uuid varchar(255) null
) collate = utf8mb4_danish_ci;

create index fn_ous_act_name_index
    on ous (active, name);

create table properties
(
    id         bigint auto_increment primary key,
    prop_key   varchar(50)  not null,
    prop_value varchar(255) not null,
    entity_id  bigint       not null
);

create index index_properties_key_value
    on properties (prop_key, prop_value);

create table registers
(
    id                          bigint                            not null primary key,
    created_at                  datetime(6)                       not null,
    created_by                  varchar(255)                      not null,
    name                        varchar(768)                      not null,
    updated_at                  datetime(6)                       null,
    updated_by                  varchar(255)                      null,
    relation_type               varchar(30)                       not null,
    version                     int                               not null,
    responsible_uuid            varchar(36)                       null,
    responsible_ou_uuid         varchar(36)                       null,
    purpose                     text                              null,
    gdpr_choices                text                              null,
    information_obligation_desc text                              null,
    information_obligation      varchar(100)                      null,
    description                 text                              null,
    emergency_plan_link         varchar(1024)                     null,
    criticality                 varchar(100)                      null,
    information_responsible     varchar(255)                      null,
    register_regarding          varchar(255)                      null,
    department                  varchar(36)                       null,
    purpose_notes               text                              null,
    data_processing_id          bigint                            null,
    package_name                varchar(50) default '0'           null,
    deleted                     bit         default b'0'          null,
    consent                     text                              null,
    status                      varchar(36) default 'NOT_STARTED' not null,
    localized_enums             varchar(255)                      null,
    constraint fk_reg_data_processing_id
        foreign key (data_processing_id) references data_processing (id)
) collate = utf8mb4_danish_ci;

create table consequence_assessments
(
    register_id                      bigint       not null primary key,
    confidentiality_registered       int          null,
    confidentiality_organisation     int          null,
    confidentiality_reason           text         null,
    integrity_registered             int          null,
    integrity_organisation           int          null,
    integrity_reason                 text         null,
    availability_registered          int          null,
    availability_organisation        int          null,
    availability_reason              text         null,
    assessment                       varchar(255) null,
    created_at                       datetime     not null,
    created_by                       varchar(255) not null,
    updated_at                       datetime     null,
    updated_by                       varchar(255) null,
    confidentiality_organisation_rep int          null,
    confidentiality_organisation_eco int          null,
    integrity_organisation_rep       int          null,
    integrity_organisation_eco       int          null,
    availability_organisation_rep    int          null,
    availability_organisation_eco    int          null,
    constraint FK_CONSEQUENCE_ASSESSMENTS_ON_REGISTER
        foreign key (register_id) references registers (id)
) collate = utf8mb4_danish_ci;

create index idx_registers_localized_enums
    on registers (localized_enums);

create index idx_registers_name
    on registers (name);

create index registers_deleted__index
    on registers (deleted);

create table relatable_tags
(
    id           bigint auto_increment primary key,
    relatable_id bigint not null,
    tag_id       bigint not null
);

create table relations
(
    id              bigint auto_increment primary key,
    relation_a_id   bigint      not null,
    relation_a_type varchar(50) not null,
    relation_b_id   bigint      not null,
    relation_b_type varchar(50) not null
) collate = utf8mb4_danish_ci;

create index relations_a_index
    on relations (relation_a_id);

create index relations_b_index
    on relations (relation_b_id);

create table settings
(
    id            bigint auto_increment primary key,
    setting_key   varchar(255) not null,
    setting_value varchar(255) not null,
    last_updated  datetime     not null,
    association   varchar(128) null,
    editable      bit          null,
    constraint setting_key
        unique (setting_key)
) collate = utf8mb4_danish_ci;

create table standard_templates
(
    identifier varchar(255)         not null primary key,
    name       varchar(255)         null,
    supporting tinyint(1) default 1 not null
) collate = utf8mb4_danish_ci;

create table standard_template_sections
(
    identifier                   varchar(255) not null primary key,
    section                      varchar(255) null,
    description                  text         null,
    parent_identifier            varchar(255) null,
    standard_template_identifier varchar(255) null,
    security_level               varchar(128) null,
    sort_key                     bigint       null,
    constraint FK_STANDARD_TEMPLATE_SECTIONS_ON_STANDARD_TEMPLATE_IDENTIFIER
        foreign key (standard_template_identifier) references standard_templates (identifier)
) collate = utf8mb4_danish_ci;

create table standard_sections
(
    id                          bigint                  not null primary key,
    version                     int                     not null,
    relation_type               varchar(255)            not null,
    name                        varchar(255)            not null,
    created_at                  datetime                not null,
    created_by                  varchar(255)            not null,
    updated_at                  datetime                null,
    updated_by                  varchar(255)            null,
    template_section_identifier varchar(255)            null,
    description                 text                    null,
    reason                      varchar(255)            null,
    status                      varchar(255)            null,
    responsible_user_uuid       varchar(36)             null,
    selected                    tinyint(1) default 1    not null,
    nsis_practice               text                    null,
    nsis_smart                  text                    null,
    deleted                     bit        default b'0' null,
    localized_enums             varchar(255)            null,
    constraint FK_STANDARD_SECTIONS_ON_TEMPLATE_SECTION_IDENTIFIER
        foreign key (template_section_identifier) references standard_template_sections (identifier)
) collate = utf8mb4_danish_ci;

create index idx_standard_sections_localized_enums
    on standard_sections (localized_enums);

create index idx_standard_sections_name
    on standard_sections (name);

create index standard_sections_deleted__index
    on standard_sections (deleted);

create table suppliers
(
    id               bigint auto_increment primary key,
    version          bigint                                   not null,
    name             varchar(255)                             not null,
    created_at       timestamp  default current_timestamp()   not null,
    created_by       varchar(100)                             not null,
    updated_at       timestamp                                not null,
    updated_by       varchar(100)                             null,
    relation_type    varchar(30)                              not null,
    status           varchar(50)                              not null,
    cvr              varchar(10)                              null,
    zip              varchar(10)                              null,
    city             varchar(255)                             null,
    country          varchar(255)                             null,
    address          varchar(255)                             null,
    contact          varchar(255)                             null,
    phone            varchar(50)                              null,
    email            varchar(255)                             null,
    description      text                                     null,
    personal_info    tinyint(1) default 0                     not null,
    data_processor   tinyint(1) default 0                     not null,
    responsible_uuid varchar(36)                              null,
    deleted          bit        default b'0'                  null,
    localized_enums  varchar(255)                             null
) collate = utf8mb4_danish_ci;

create table assets_suppliers
(
    id                     bigint auto_increment primary key,
    asset_id               bigint       not null,
    supplier_id            bigint       not null,
    service                text         null,
    third_country_transfer varchar(255) null,
    acceptance_basis       text         null,
    constraint fk_assets_suppliers_asset_id
        foreign key (asset_id) references assets (id),
    constraint fk_assets_suppliers_supplier_id
        foreign key (supplier_id) references suppliers (id)
) collate = utf8mb4_danish_ci;

create index idx_suppliers_localized_enums
    on suppliers (localized_enums);

create index suppliers_deleted__index
    on suppliers (deleted);

create index suppliers_name_index
    on suppliers (name);

create table tags
(
    id    bigint auto_increment primary key,
    value varchar(255) null,
    constraint uq_tags_value
        unique (value)
) collate = utf8mb4_danish_ci;

create table tasks
(
    id                   bigint           not null primary key,
    created_at           datetime(6)      not null,
    created_by           varchar(255)     not null,
    name                 varchar(255)     not null,
    updated_at           datetime(6)      null,
    updated_by           varchar(255)     null,
    relation_type        varchar(30)      not null,
    version              int              not null,
    description          varchar(500)     null,
    next_deadline        date             null,
    notify_responsible   bit              null,
    repetition           varchar(100)     null,
    task_type            varchar(100)     null,
    responsible_uuid     varchar(36)      null,
    responsible_ou_uuid  varchar(36)      null,
    responsible_notified bit              null,
    deleted              bit default b'0' null,
    localized_enums      varchar(255)     null
) collate = utf8mb4_danish_ci;

create table task_logs
(
    id                       bigint           not null primary key,
    created_at               datetime(6)      not null,
    created_by               varchar(255)     not null,
    name                     varchar(255)     not null,
    updated_at               datetime(6)      null,
    updated_by               varchar(255)     null,
    relation_type            varchar(30)      not null,
    version                  int              not null,
    comment                  varchar(500)     null,
    task_id                  bigint           null,
    responsible_user_user_id varchar(255)     null,
    responsible_o_u_name     varchar(255)     null,
    deadline                 datetime(6)      not null,
    completed                datetime(6)      not null,
    current_description      varchar(500)     null,
    document_id              bigint           null,
    documentation_link       varchar(500)     null,
    task_result              varchar(255)     null,
    deleted                  bit default b'0' null,
    localized_enums          varchar(255)     null,
    constraint fk_task_logs_document_id
        foreign key (document_id) references documents (id),
    constraint fk_task_logs_task_id
        foreign key (task_id) references tasks (id)
) collate = utf8mb4_danish_ci;

create index task_logs_deleted__index
    on task_logs (deleted);

create index idx_tasks_localized_enums
    on tasks (localized_enums);

create index tasks_deleted__index
    on tasks (deleted);

create index tasks_name_index
    on tasks (name);

create table threat_catalogs
(
    identifier varchar(255) not null primary key,
    name       varchar(255) null
) collate = utf8mb4_danish_ci;

create table threat_assessments
(
    id                                     bigint           not null primary key,
    version                                int              not null,
    relation_type                          varchar(255)     not null,
    name                                   varchar(255)     not null,
    created_at                             datetime         not null,
    created_by                             varchar(255)     not null,
    updated_at                             datetime         null,
    updated_by                             varchar(255)     null,
    responsible_uuid                       varchar(36)      null,
    responsible_ou_uuid                    varchar(36)      null,
    threat_assessment_type                 varchar(255)     null,
    threat_catalog_identifier              varchar(255)     null,
    registered                             bit              null,
    organisation                           bit              null,
    inherit                                bit              null,
    assessment                             varchar(255)     null,
    inherited_confidentiality_registered   int              null,
    inherited_confidentiality_organisation int              null,
    inherited_integrity_registered         int              null,
    inherited_integrity_organisation       int              null,
    inherited_availability_registered      int              null,
    inherited_availability_organisation    int              null,
    deleted                                bit default b'0' null,
    localized_enums                        varchar(255)     null,
    constraint FK_THREAT_ASSESSMENTS_ON_THREAT_CATALOG_IDENTIFIER
        foreign key (threat_catalog_identifier) references threat_catalogs (identifier)
) collate = utf8mb4_danish_ci;

create table custom_threats
(
    id                   bigint       not null primary key,
    threat_type          varchar(255) null,
    description          varchar(255) null,
    threat_assessment_id bigint       not null,
    constraint fk_custom_threats_threat_assessment
        foreign key (threat_assessment_id) references threat_assessments (id)
            on delete cascade
) collate = utf8mb4_danish_ci;

create index threat_assessments_deleted__index
    on threat_assessments (deleted);

create table threat_catalog_threats
(
    identifier                varchar(255) not null primary key,
    thread_catalog_identifier varchar(255) null,
    threat_type               varchar(255) null,
    description               text         null,
    rights                    bit          null,
    constraint FK_THREAT_CATALOG_THREATS_ON_THREAD_CATALOG_IDENTIFIER
        foreign key (thread_catalog_identifier) references threat_catalogs (identifier)
) collate = utf8mb4_danish_ci;

create table threat_assessment_responses
(
    id                           bigint auto_increment primary key,
    not_relevant                 bit          null,
    probability                  int          null,
    confidentiality_registered   int          null,
    confidentiality_organisation int          null,
    integrity_registered         int          null,
    integrity_organisation       int          null,
    availability_registered      int          null,
    availability_organisation    int          null,
    residual_risk_probability    int          null,
    residual_risk_consequence    int          null,
    problem                      varchar(255) null,
    existing_measures            varchar(255) null,
    method                       varchar(255) null,
    elaboration                  varchar(255) null,
    threat_assessment_id         bigint       not null,
    custom_threat_id             bigint       null,
    threat_catalog_threat_id     varchar(255) null,
    constraint fk_threat_assessment_responses_custom_threat
        foreign key (custom_threat_id) references custom_threats (id)
            on delete cascade,
    constraint fk_threat_assessment_responses_threat_assessment
        foreign key (threat_assessment_id) references threat_assessments (id)
            on delete cascade,
    constraint fk_threat_assessment_responses_threat_catalog_threats
        foreign key (threat_catalog_threat_id) references threat_catalog_threats (identifier)
            on delete cascade
) collate = utf8mb4_danish_ci;

create table tia
(
    id                                         bigint auto_increment primary key,
    asset_id                                   bigint        not null,
    forward_information_other_suppliers        varchar(255)  null,
    forward_information_other_suppliers_detail varchar(1024) null,
    expected_transfer_duration                 varchar(1024) null,
    transfer_case_description                  varchar(1024) null,
    access_type                                varchar(255)  null,
    information_types                          text          null,
    registered_categories                      text          null,
    technical_security_measures                text          null,
    organizational_security_measures           text          null,
    contractual_security_measures              text          null,
    conclusion                                 varchar(1024) null,
    assessment                                 varchar(255)  null,
    constraint fk_tia_asset_id
        foreign key (asset_id) references assets (id)
) collate = utf8mb4_danish_ci;

create table users
(
    uuid    varchar(36)  not null primary key,
    active  bit          null,
    name    varchar(255) null,
    email   varchar(255) null,
    user_id varchar(255) null
) collate = utf8mb4_danish_ci;

create table assets_users_mapping
(
    asset_id  bigint      not null,
    user_uuid varchar(36) not null,
    primary key (asset_id, user_uuid),
    constraint fk_asset_id
        foreign key (asset_id) references assets (id),
    constraint fk_users_uuid
        foreign key (user_uuid) references users (uuid)
) collate = utf8mb4_danish_ci;

create table positions
(
    id        bigint auto_increment primary key,
    name      varchar(255) null,
    ou_uuid   varchar(255) null,
    user_uuid varchar(255) null,
    constraint fk_positions_user_uuid
        foreign key (user_uuid) references users (uuid)
) collate = utf8mb4_danish_ci;

create table user_properties
(
    id         bigint auto_increment primary key,
    prop_key   varchar(50)  not null,
    prop_value varchar(255) not null,
    user_uuid  varchar(36)  not null,
    constraint fk_user_properties_user_uuid
        foreign key (user_uuid) references users (uuid)
) collate = utf8mb4_danish_ci;

create index index_user_properties_key_value
    on user_properties (prop_key, prop_value);

create index fn_users_act_name_index
    on users (active, name);

create index users_email_index
    on users (email);

create index users_userid_index
    on users (user_id);

