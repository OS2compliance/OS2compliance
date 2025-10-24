
CREATE TABLE documents_tag
(
    document_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (document_id, tag_id),
    CONSTRAINT fk_document_tag_document_id FOREIGN KEY (document_id) REFERENCES documents (id) ON DELETE CASCADE,
    CONSTRAINT fk_document_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_document_tag_tag_document (tag_id, document_id)
) collate = utf8mb4_danish_ci;

CREATE TABLE tasks_tag
(
    task_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (task_id, tag_id),
    CONSTRAINT fk_task_tag_task_id FOREIGN KEY (task_id) REFERENCES tasks (id) ON DELETE CASCADE,
    CONSTRAINT fk_task_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_task_tag_tag_task (tag_id, task_id)
) COLLATE = utf8mb4_danish_ci;

-- Migrate task tags to new table
INSERT INTO tasks_tag (task_id, tag_id)
SELECT DISTINCT rt.relatable_id, rt.tag_id
FROM relatable_tags rt
WHERE EXISTS (SELECT 1 FROM tasks t WHERE t.id = rt.relatable_id)
  AND EXISTS (SELECT 1 FROM tags tg WHERE tg.id = rt.tag_id);

-- migrate document tags to new table
INSERT INTO documents_tag (document_id, tag_id)
SELECT DISTINCT rt.relatable_id, rt.tag_id
FROM relatable_tags rt
WHERE EXISTS (SELECT 1 FROM documents d WHERE d.id = rt.relatable_id)
  AND EXISTS (SELECT 1 FROM tags tg WHERE tg.id = rt.tag_id);

-- drop old tag table
DROP TABLE relatable_tags;

CREATE TABLE asset_tag
(
    asset_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (asset_id, tag_id),
    CONSTRAINT fk_asset_tag_asset_id FOREIGN KEY (asset_id) REFERENCES assets (id) ON DELETE CASCADE,
    CONSTRAINT fk_asset_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_asset_tag_tag_asset (tag_id, asset_id)
) collate = utf8mb4_danish_ci;

CREATE TABLE registers_tag
(
    register_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (register_id, tag_id),
    CONSTRAINT fk_register_tag_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_register_tag_tag_register (tag_id, register_id)
) collate = utf8mb4_danish_ci;

CREATE TABLE suppliers_tag
(
    supplier_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (supplier_id, tag_id),
    CONSTRAINT fk_supplier_tag_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE CASCADE,
    CONSTRAINT fk_supplier_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_supplier_tag_tag_supplier (tag_id, supplier_id)
) collate = utf8mb4_danish_ci;

CREATE TABLE threat_assessments_tag
(
    threat_assessment_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (threat_assessment_id, tag_id),
    CONSTRAINT fk_threat_assessments_tag_threat_assessments_id FOREIGN KEY (threat_assessment_id) REFERENCES threat_assessments (id) ON DELETE CASCADE,
    CONSTRAINT fk_threat_assessments_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_threat_assessments_tag_tag_threat_assessments (tag_id, threat_assessment_id)
) collate = utf8mb4_danish_ci;

CREATE TABLE dpia_tag
(
    dpia_id BIGINT NOT NULL,
    tag_id  BIGINT NOT NULL,
    PRIMARY KEY (dpia_id, tag_id),
    CONSTRAINT fk_dpia_tag_dpia_id FOREIGN KEY (dpia_id) REFERENCES dpia (id) ON DELETE CASCADE,
    CONSTRAINT fk_dpia_tag_tag_id FOREIGN KEY (tag_id) REFERENCES tags (id) ON DELETE CASCADE,
    INDEX idx_dpia_tag_tag_dpia (tag_id, dpia_id)
) collate = utf8mb4_danish_ci;
