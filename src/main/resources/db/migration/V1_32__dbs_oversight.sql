CREATE TABLE dbs_oversight (
    id                            BIGINT AUTO_INCREMENT NOT NULL,
    dbs_id                        BIGINT NOT NULL,
    name                          VARCHAR(768) NOT NULL,

    dbs_supplier_id               BIGINT NOT NULL,
    locked                        BOOLEAN NOT NULL default false,
    task_created                  BOOLEAN NOT NULL default false,
    created                       DATETIME(6) NULL,
    CONSTRAINT dbs_oversight_pk PRIMARY KEY (id),
    CONSTRAINT dbs_oversight_unique UNIQUE KEY (dbs_id),
    CONSTRAINT dbs_oversight_dbs_supplier_FK FOREIGN KEY (dbs_supplier_id) REFERENCES dbs_supplier(id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;