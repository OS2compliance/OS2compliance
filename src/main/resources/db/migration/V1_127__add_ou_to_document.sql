ALTER TABLE documents
    ADD COLUMN responsible_ou_uuid VARCHAR(36) NULL,
    ADD COLUMN department_uuid     VARCHAR(36) NULL;

ALTER TABLE documents
    ADD CONSTRAINT fk_documents_responsible_ou_uuid FOREIGN KEY (responsible_ou_uuid) REFERENCES ous (uuid),
    ADD CONSTRAINT fk_documents_department_uuid FOREIGN KEY (department_uuid) REFERENCES ous (uuid);

ALTER TABLE documents_aud
    ADD COLUMN responsible_ou_uuid VARCHAR(36) NULL,
    ADD COLUMN department_uuid     VARCHAR(36) NULL;
