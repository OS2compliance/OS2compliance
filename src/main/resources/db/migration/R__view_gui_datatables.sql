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
    (ts.id IS NOT NULL) as completed, ts.task_result AS result,
    concat(COALESCE(t.localized_enums, ''), ' ', COALESCE(ts.localized_enums, ' ')) as localized_enums
FROM tasks t
    LEFT JOIN task_logs ts on ts.task_id = t.id
WHERE
    t.deleted = false AND
    (ts.id IS NULL OR ts.id = (SELECT MAX(id) FROM task_logs WHERE task_id = t.id));

CREATE OR REPLACE
VIEW view_gridjs_registers AS
SELECT
    r.id,
    r.name,
    r.responsible_uuid,
    r.responsible_ou_uuid,
    r.updated_at,
    ca.assessment as consequence,
    ta.assessment as risk,
    concat(COALESCE(r.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    r.status
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
    a.supplier_id,
    a.asset_type,
    a.responsible_uuid,
    a.updated_at,
    a.asset_status,
    ta.assessment,
    concat(COALESCE(a.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    IF(properties.prop_value IS null, 0, 1) AS kitos
FROM assets a
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
    d.localized_enums
FROM
    documents d
WHERE d.deleted=false;
