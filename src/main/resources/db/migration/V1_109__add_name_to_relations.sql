-- VARCHAR(768) matcher de bredeste kilde-kolonner (registers, assets, dbs_asset, dbs_oversight, incidents, dpia).
ALTER TABLE relations
    ADD COLUMN relation_a_name VARCHAR(768) NULL,
    ADD COLUMN relation_b_name VARCHAR(768) NULL;

-- Migrate existing data by joining each type to its respective table.
-- All Relatable subclasses have a `name` column in their own table.
-- LEFT(..., 768): kunde-databaser kan indeholde navne laengere end skemaet tilsiger (skema-drift),
-- saa vi trunkerer defensivt i stedet for at lade migrationen fejle paa enkelt-raekker.

UPDATE relations r
    LEFT JOIN suppliers s ON r.relation_a_id = s.id AND r.relation_a_type = 'SUPPLIER'
    LEFT JOIN contacts c ON r.relation_a_id = c.id AND r.relation_a_type = 'CONTACT'
    LEFT JOIN tasks t ON r.relation_a_id = t.id AND r.relation_a_type = 'TASK'
    LEFT JOIN documents d ON r.relation_a_id = d.id AND r.relation_a_type = 'DOCUMENT'
    LEFT JOIN task_logs tl ON r.relation_a_id = tl.id AND r.relation_a_type = 'TASK_LOG'
    LEFT JOIN registers reg ON r.relation_a_id = reg.id AND r.relation_a_type = 'REGISTER'
    LEFT JOIN assets a ON r.relation_a_id = a.id AND r.relation_a_type = 'ASSET'
    LEFT JOIN standard_sections ss ON r.relation_a_id = ss.id AND r.relation_a_type = 'STANDARD_SECTION'
    LEFT JOIN threat_assessments ta ON r.relation_a_id = ta.id AND r.relation_a_type = 'THREAT_ASSESSMENT'
    LEFT JOIN threat_assessment_responses tar ON r.relation_a_id = tar.id AND r.relation_a_type = 'THREAT_ASSESSMENT_RESPONSE'
    LEFT JOIN precautions p ON r.relation_a_id = p.id AND r.relation_a_type = 'PRECAUTION'
    LEFT JOIN dbs_asset da ON r.relation_a_id = da.id AND r.relation_a_type = 'DBSASSET'
    LEFT JOIN dbs_oversight dbo ON r.relation_a_id = dbo.id AND r.relation_a_type = 'DBSOVERSIGHT'
    LEFT JOIN incidents i ON r.relation_a_id = i.id AND r.relation_a_type = 'INCIDENT'
    LEFT JOIN dpia dp ON r.relation_a_id = dp.id AND r.relation_a_type = 'DPIA'
SET r.relation_a_name = LEFT(COALESCE(s.name, c.name, t.name, d.name, tl.name, reg.name, a.name, ss.name, ta.name, tar.name, p.name, da.name, dbo.name, i.name, dp.name), 768);

UPDATE relations r
    LEFT JOIN suppliers s ON r.relation_b_id = s.id AND r.relation_b_type = 'SUPPLIER'
    LEFT JOIN contacts c ON r.relation_b_id = c.id AND r.relation_b_type = 'CONTACT'
    LEFT JOIN tasks t ON r.relation_b_id = t.id AND r.relation_b_type = 'TASK'
    LEFT JOIN documents d ON r.relation_b_id = d.id AND r.relation_b_type = 'DOCUMENT'
    LEFT JOIN task_logs tl ON r.relation_b_id = tl.id AND r.relation_b_type = 'TASK_LOG'
    LEFT JOIN registers reg ON r.relation_b_id = reg.id AND r.relation_b_type = 'REGISTER'
    LEFT JOIN assets a ON r.relation_b_id = a.id AND r.relation_b_type = 'ASSET'
    LEFT JOIN standard_sections ss ON r.relation_b_id = ss.id AND r.relation_b_type = 'STANDARD_SECTION'
    LEFT JOIN threat_assessments ta ON r.relation_b_id = ta.id AND r.relation_b_type = 'THREAT_ASSESSMENT'
    LEFT JOIN threat_assessment_responses tar ON r.relation_b_id = tar.id AND r.relation_b_type = 'THREAT_ASSESSMENT_RESPONSE'
    LEFT JOIN precautions p ON r.relation_b_id = p.id AND r.relation_b_type = 'PRECAUTION'
    LEFT JOIN dbs_asset da ON r.relation_b_id = da.id AND r.relation_b_type = 'DBSASSET'
    LEFT JOIN dbs_oversight dbo ON r.relation_b_id = dbo.id AND r.relation_b_type = 'DBSOVERSIGHT'
    LEFT JOIN incidents i ON r.relation_b_id = i.id AND r.relation_b_type = 'INCIDENT'
    LEFT JOIN dpia dp ON r.relation_b_id = dp.id AND r.relation_b_type = 'DPIA'
SET r.relation_b_name = LEFT(COALESCE(s.name, c.name, t.name, d.name, tl.name, reg.name, a.name, ss.name, ta.name, tar.name, p.name, da.name, dbo.name, i.name, dp.name), 768);
