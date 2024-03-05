CREATE OR REPLACE
VIEW view_gridjs_suppliers AS
SELECT
	s.id,
	s.name,
	(SELECT COUNT(1) FROM assets a WHERE a.supplier_id=s.id) AS solution_count,
    s.updated_at AS updated,
    s.status,
    s.localized_enums
FROM
	suppliers s
WHERE s.deleted = false;

CREATE OR REPLACE
VIEW view_gridjs_tasks AS
SELECT
    t.id,
    t.name,
    t.task_type,
    t.responsible_uuid,
    t.responsible_ou_uuid,
    t.next_deadline,
    t.repetition,
    (ts.id IS NOT NULL) as completed,
    ts.task_result AS result,
    concat(COALESCE(t.localized_enums, ''), ' ', COALESCE(ts.localized_enums, ' ')) as localized_enums,
    GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value ASC SEPARATOR ',') as tags
FROM tasks t
    LEFT JOIN task_logs ts on ts.task_id = t.id
    LEFT JOIN relatable_tags rt on rt.relatable_id = t.id
    LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE
    t.deleted = false AND
    (ts.id IS NULL OR ts.id = (SELECT MAX(id) FROM task_logs WHERE task_id = t.id))
GROUP BY t.id;

CREATE OR REPLACE
VIEW view_gridjs_registers AS
SELECT
    r.id,
    r.name,
    r.responsible_uuid,
    r.responsible_ou_uuid,
    r.department,
    r.updated_at,
    ca.assessment as consequence,
    ta.assessment as risk,
    concat(COALESCE(r.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    r.status,
    (SELECT COUNT(rel.id) FROM relations rel WHERE (rel.relation_a_id = r.id OR rel.relation_b_id = r.id) AND (rel.relation_a_type = 'ASSET' OR rel.relation_b_type = 'ASSET')) AS asset_count
FROM registers r
LEFT JOIN consequence_assessments ca on ca.register_id = r.id
LEFT JOIN threat_assessments ta ON ta.id = (
    SELECT MAX(tb.id) FROM threat_assessments tb
        JOIN relations rel ON rel.relation_a_id = r.id or rel.relation_b_id = r.id
        WHERE rel.relation_b_id = tb.id OR rel.relation_a_id = tb.id)
WHERE r.deleted = false
;


CREATE OR REPLACE
VIEW view_gridjs_assets AS
SELECT
    a.id,
    a.name,
    s.name as supplier,
    a.asset_type,
    a.responsible_uuid,
    u.name as responsible_user_name,
    a.updated_at,
    a.asset_status,
    ta.assessment,
    concat(COALESCE(a.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    IF(properties.prop_value IS null, 0, 1) AS kitos,
    (SELECT count(*) FROM relations r WHERE (relation_a_id = a.id and relation_a_type = 'ASSET' and relation_b_type = 'REGISTER') OR (relation_b_id = a.id AND relation_a_type = 'REGISTER' and relation_b_type = 'ASSET')) as registers
FROM assets a
    LEFT JOIN users u on u.uuid = a.responsible_uuid
    LEFT JOIN suppliers s on s.id = a.supplier_id
    LEFT JOIN properties ON properties.entity_id = a.id
    LEFT JOIN threat_assessments ta ON ta.id = (
            SELECT MAX(tb.id) FROM threat_assessments tb
            JOIN relations r ON r.relation_a_id = a.id or r.relation_b_id = a.id
            WHERE r.relation_b_id = tb.id OR r.relation_a_id = tb.id
    )
WHERE a.deleted = false AND (properties.prop_key = 'kitos_uuid' OR properties.prop_key IS NULL);

CREATE OR REPLACE
VIEW view_gridjs_assessments AS
SELECT
    t.id,
    t.name,
    t.responsible_uuid,
    t.responsible_ou_uuid,
    t.threat_assessment_type as type,
    t.updated_at as date,
    t.assessment,
    t.localized_enums,
    (SELECT COUNT(r.id) FROM relations r WHERE (r.relation_a_id = t.id OR r.relation_b_id = t.id) AND (r.relation_a_type = 'TASK' OR r.relation_b_type = 'TASK')) AS tasks
FROM
    threat_assessments t
WHERE t.deleted = false;

CREATE OR REPLACE
VIEW view_gridjs_documents AS
SELECT
    d.id,
    d.name,
    d.document_type,
    d.responsible_uuid,
    d.next_revision,
    d.status,
    d.localized_enums,
    GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value ASC SEPARATOR ',') as tags
FROM documents d
    LEFT JOIN relatable_tags rt on rt.relatable_id = d.id
    LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE d.deleted=false
GROUP BY d.id;
