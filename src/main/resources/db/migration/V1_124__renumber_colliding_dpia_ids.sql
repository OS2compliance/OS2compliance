-- Flyt dpia-raekker hvis id ogsaa findes i en anden Relatable-tabel, og skub next_val forbi hoejeste
-- id i brug, saa generatoren ikke deler et id ud der er i brug. Se commit-beskrivelsen for hvordan
-- V1_52__fix_dpia_ids.sql efterlod begge dele. FK-tjekket er slaaet fra fordi boernetabellerne har
-- UPDATE RESTRICT paa dpia (id), saa hverken foraelder eller barn kan opdateres foerst.

SET FOREIGN_KEY_CHECKS = 0;

-- Temp-tabellerne lever paa forbindelsen, ikke i schemaet
DROP TEMPORARY TABLE IF EXISTS dpia_id_flyt;
DROP TEMPORARY TABLE IF EXISTS uklar_properties;
DROP TEMPORARY TABLE IF EXISTS kollision;
DROP TEMPORARY TABLE IF EXISTS relatable_id_type;

START TRANSACTION;

-- Laas generatoren, saa ingen instans tildeler id'er mens de nye beregnes
SELECT next_val INTO @next_val FROM hibernate_sequences WHERE sequence_name = 'default' FOR UPDATE;

CREATE TEMPORARY TABLE relatable_id_type
(
    id            BIGINT       NOT NULL,
    relation_type VARCHAR(255) NOT NULL,
    KEY (id)
);
INSERT INTO relatable_id_type (id, relation_type)
SELECT id, relation_type FROM assets
UNION ALL SELECT id, relation_type FROM contacts
UNION ALL SELECT id, relation_type FROM dbs_asset
UNION ALL SELECT id, relation_type FROM documents
UNION ALL SELECT id, relation_type FROM dpia
UNION ALL SELECT id, relation_type FROM incidents
UNION ALL SELECT id, relation_type FROM precautions
UNION ALL SELECT id, relation_type FROM registers
UNION ALL SELECT id, relation_type FROM standard_sections
UNION ALL SELECT id, relation_type FROM suppliers
UNION ALL SELECT id, relation_type FROM tasks
UNION ALL SELECT id, relation_type FROM task_logs
UNION ALL SELECT id, relation_type FROM threat_assessments
UNION ALL SELECT id, relation_type FROM threat_assessment_responses;

-- custom_threats taeller med, den traekker paa samme generator
SELECT GREATEST(COALESCE(MAX(id), 0), COALESCE((SELECT MAX(id) FROM custom_threats), 0))
INTO @max_id
FROM relatable_id_type;

-- Generatoren kan naa op til next_val + 1 fra den vaerdi der staar nu, saa nye id'er skal ligge over
SET @base = GREATEST(COALESCE(@next_val, 0) + 1, @max_id);

CREATE TEMPORARY TABLE kollision
(
    id BIGINT NOT NULL PRIMARY KEY
);
INSERT INTO kollision (id)
SELECT id
FROM relatable_id_type
GROUP BY id
HAVING COUNT(*) > 1;

-- properties har ingen typekolonne, saa ejeren kan kun udledes af noeglen, og kun for de noegler
-- koden selv skriver. Tilhoerer en raekke ikke praecis en af de kolliderende typer, lades
-- kollisionen staa.
CREATE TEMPORARY TABLE uklar_properties
(
    id BIGINT NOT NULL PRIMARY KEY
);
INSERT INTO uklar_properties (id)
SELECT DISTINCT p.entity_id
FROM properties p
         JOIN kollision k ON k.id = p.entity_id
WHERE (SELECT COUNT(*)
       FROM (SELECT 'linked_asset' AS prop_key, 'TASK' AS ejer
             UNION ALL SELECT 'linked_doc', 'TASK'
             UNION ALL SELECT 'linked_threat', 'TASK'
             UNION ALL SELECT 'linked_dpia', 'TASK'
             UNION ALL SELECT 'cvr_update', 'SUPPLIER'
             UNION ALL SELECT 'cvr_updated_at', 'SUPPLIER'
             UNION ALL SELECT 'asset_assessment', 'REGISTER'
             UNION ALL SELECT 'kitos_uuid', 'ASSET'
             UNION ALL SELECT 'kitos_uuid', 'SUPPLIER'
             UNION ALL SELECT 'kitos_usage_uuid', 'ASSET'
             UNION ALL SELECT 'old_kitos_usage_uuid', 'ASSET'
             UNION ALL SELECT 'kitos_risk_last_sync', 'ASSET'
             UNION ALL SELECT 'kitos_dpia_last_sync', 'ASSET') n
                JOIN relatable_id_type t ON t.id = p.entity_id AND t.relation_type = n.ejer
       WHERE n.prop_key = p.prop_key) <> 1;

CREATE TEMPORARY TABLE dpia_id_flyt
(
    gammelt_id BIGINT NOT NULL PRIMARY KEY,
    nyt_id     BIGINT NOT NULL
);
INSERT INTO dpia_id_flyt (gammelt_id, nyt_id)
SELECT d.id,
       @base + ROW_NUMBER() OVER (ORDER BY d.id)
FROM dpia d
         JOIN kollision k ON k.id = d.id
WHERE d.id NOT IN (SELECT id FROM uklar_properties);

UPDATE dpia d JOIN dpia_id_flyt f ON f.gammelt_id = d.id SET d.id = f.nyt_id;
UPDATE dpia_asset c JOIN dpia_id_flyt f ON f.gammelt_id = c.dpia_id SET c.dpia_id = f.nyt_id;
UPDATE dpia_report c JOIN dpia_id_flyt f ON f.gammelt_id = c.dpia_id SET c.dpia_id = f.nyt_id;
UPDATE dpia_response_section c JOIN dpia_id_flyt f ON f.gammelt_id = c.dpia_id SET c.dpia_id = f.nyt_id;
UPDATE dpia_screening c JOIN dpia_id_flyt f ON f.gammelt_id = c.dpia_id SET c.dpia_id = f.nyt_id;
UPDATE dpia_tag c JOIN dpia_id_flyt f ON f.gammelt_id = c.dpia_id SET c.dpia_id = f.nyt_id;

-- Uden FK: relations disambigueres af typekolonnen, og linked_dpia holder dpia-id'et som tekst
UPDATE relations r JOIN dpia_id_flyt f ON f.gammelt_id = r.relation_a_id
SET r.relation_a_id = f.nyt_id
WHERE r.relation_a_type = 'DPIA';
UPDATE relations r JOIN dpia_id_flyt f ON f.gammelt_id = r.relation_b_id
SET r.relation_b_id = f.nyt_id
WHERE r.relation_b_type = 'DPIA';
UPDATE properties p JOIN dpia_id_flyt f ON p.prop_value = CAST(f.gammelt_id AS CHAR)
SET p.prop_value = CAST(f.nyt_id AS CHAR)
WHERE p.prop_key = 'linked_dpia';

-- INSERT IGNORE foerst: mangler generatorraekken, rammer UPDATE'en ingenting, og Hibernate seeder
-- fra initialValue ved naeste opstart og deler id'er ud der er i brug. GREATEST sikrer at next_val
-- kun kan stige.
SELECT COALESCE(MAX(nyt_id), 0) INTO @flyttet_max FROM dpia_id_flyt;
INSERT IGNORE INTO hibernate_sequences (sequence_name, next_val)
VALUES ('default', GREATEST(@max_id + 50, @flyttet_max + 50));
UPDATE hibernate_sequences
SET next_val = GREATEST(next_val, @max_id + 50, @flyttet_max + 50)
WHERE sequence_name = 'default';

COMMIT;

DROP TEMPORARY TABLE dpia_id_flyt;
DROP TEMPORARY TABLE uklar_properties;
DROP TEMPORARY TABLE kollision;
DROP TEMPORARY TABLE relatable_id_type;

SET FOREIGN_KEY_CHECKS = 1;
