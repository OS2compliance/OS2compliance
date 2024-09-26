CREATE TABLE dbs_oversight (
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    dbs_id                        BIGINT NOT NULL,
--    created_at                    DATETIME(6) NOT NULL,
--    created_by                    VARCHAR(255) NOT NULL,
    name                          VARCHAR(768) NOT NULL,
--    updated_at                    DATETIME(6) NULL,
--    updated_by                    VARCHAR(255) NULL,
--    relation_type                 VARCHAR(30) NOT NULL,
--    version                       INT NOT NULL,
--    deleted                       BOOLEAN DEFAULT FALSE null,
--    localized_enums               VARCHAR(255) NULL,

    dbs_supplier_id               BIGINT NOT NULL,
    locked                        BOOLEAN NOT NULL default false,
    task_created                  BOOLEAN NOT NULL default false,
    created                       DATETIME(6) NULL,
    CONSTRAINT dbs_oversight_pk PRIMARY KEY (id),
    CONSTRAINT dbs_oversight_unique UNIQUE KEY (dbs_id),
    CONSTRAINT dbs_oversight_dbs_supplier_FK FOREIGN KEY (dbs_supplier_id) REFERENCES dbs_supplier(id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;