CREATE TABLE dbs_supplier (
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    dbs_id                        BIGINT NOT NULL,
    name                          VARCHAR(100) NULL,
    next_revision                 DATETIME NULL,

    CONSTRAINT dbs_supplier_pk PRIMARY KEY (id),
    CONSTRAINT dbs_supplier_unique UNIQUE KEY (dbs_id)
);

CREATE TABLE dbs_asset (
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    dbs_id                        BIGINT NOT NULL,
    created_at                    DATETIME(6) NOT NULL,
    created_by                    VARCHAR(255) NOT NULL,
    name                          VARCHAR(768) NOT NULL,
    updated_at                    DATETIME(6) NULL,
    updated_by                    VARCHAR(255) NULL,
    relation_type                 VARCHAR(30) NOT NULL,
    version                       INT NOT NULL,
    deleted                       BOOLEAN DEFAULT FALSE null,
    localized_enums               VARCHAR(255) NULL,

    applicable                    BOOLEAN NULL,
    dbs_supplier_id               BIGINT NOT NULL,

    CONSTRAINT dbs_asset_pk PRIMARY KEY (id),
    CONSTRAINT dbs_asset_unique UNIQUE KEY (dbs_id),
    CONSTRAINT dbs_asset_dbs_supplier_FK FOREIGN KEY (dbs_supplier_id) REFERENCES dbs_supplier(id) ON DELETE CASCADE
);