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
    (CASE WHEN t.repetition = 'NONE' THEN 10
        WHEN t.repetition = 'MONTHLY' THEN 2
        WHEN t.repetition = 'QUARTERLY' THEN 3
        WHEN t.repetition = 'HALF_YEARLY' THEN 4
        WHEN t.repetition = 'YEARLY' THEN 5
        WHEN t.repetition = 'EVERY_SECOND_YEAR' THEN 6
        WHEN t.repetition = 'EVERY_THIRD_YEAR' THEN 7
        END) as repetition_order,
    ts.task_result AS result,
    (CASE WHEN ts.task_result = 'NO_ERROR' THEN 1
          WHEN ts.task_result = 'NO_CRITICAL_ERROR' THEN 2
          WHEN ts.task_result = 'CRITICAL_ERROR' THEN 3
        END) as task_result_order,
    (ts.id IS NOT NULL AND t.task_type = 'TASK') as completed,
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
    GROUP_CONCAT(DISTINCT u.name SEPARATOR ', ') as responsible_user_names,
    GROUP_CONCAT(DISTINCT u.uuid SEPARATOR ',') as responsible_user_uuids,
    GROUP_CONCAT(DISTINCT ou.name SEPARATOR ', ') as responsible_ou_names,
    GROUP_CONCAT(DISTINCT d.name SEPARATOR ', ') as department_names,
    r.updated_at,
    ca.assessment as consequence,
    (CASE WHEN ca.assessment = 'GREEN' THEN 1
          WHEN ca.assessment = 'YELLOW' THEN 2
          WHEN ca.assessment = 'RED' THEN 3
        END) as consequence_order,
    ta.assessment as risk,
    (CASE WHEN ta.assessment = 'GREEN' THEN 1
          WHEN ta.assessment = 'LIGHT_GREEN' THEN 2
          WHEN ta.assessment = 'YELLOW' THEN 3
          WHEN ta.assessment = 'ORANGE' THEN 4
          WHEN ta.assessment = 'RED' THEN 5
        END) as risk_order,
    concat(COALESCE(r.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    r.status,
    (CASE WHEN r.status = 'NOT_STARTED' THEN 1
          WHEN r.status = 'IN_PROGRESS' THEN 2
          WHEN r.status = 'READY' THEN 3
        END) as status_order,
    (SELECT COUNT(rel.id) FROM relations rel WHERE (rel.relation_a_id = r.id OR rel.relation_b_id = r.id) AND (rel.relation_a_type = 'ASSET' OR rel.relation_b_type = 'ASSET')) AS asset_count
FROM registers r
LEFT JOIN consequence_assessments ca on ca.register_id = r.id
LEFT JOIN threat_assessments ta ON ta.id = (
    SELECT MAX(tb.id) FROM threat_assessments tb
        JOIN relations rel ON rel.relation_a_id = r.id or rel.relation_b_id = r.id
        WHERE rel.relation_b_id = tb.id OR rel.relation_a_id = tb.id)
LEFT JOIN registers_responsible_users_mapping rum ON rum.register_id = r.id
LEFT JOIN users u ON rum.user_uuid = u.uuid
LEFT JOIN registers_responsible_ous_mapping roum ON roum.register_id = r.id
LEFT JOIN ous ou ON roum.ou_uuid = ou.uuid
LEFT JOIN registers_departments_mapping rdm ON rdm.register_id = r.id
LEFT JOIN ous d ON rdm.ou_uuid = d.uuid
WHERE r.deleted = false
GROUP BY r.id;


CREATE OR REPLACE
VIEW view_gridjs_assets AS
SELECT
    a.id,
    a.name,
    s.name as supplier,
    a.asset_type,
    GROUP_CONCAT(u.name SEPARATOR ', ') as responsible_user_names,
    GROUP_CONCAT(u.uuid SEPARATOR ',') as responsible_user_uuids,
    a.updated_at,
    a.asset_status,
    (CASE WHEN a.asset_status = 'NOT_STARTED' THEN 1
          WHEN a.asset_status = 'ON_GOING' THEN 2
          WHEN a.asset_status = 'READY' THEN 3
        END) as asset_status_order,
    ta.assessment,
    (CASE WHEN ta.assessment = 'GREEN' THEN 1
          WHEN ta.assessment = 'LIGHT_GREEN' THEN 2
          WHEN ta.assessment = 'YELLOW' THEN 3
          WHEN ta.assessment = 'ORANGE' THEN 4
          WHEN ta.assessment = 'RED' THEN 5
        END) as assessment_order,
    concat(COALESCE(a.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
    IF(properties.prop_value IS null, 0, 1) AS kitos,
    CASE
        WHEN EXISTS (
            SELECT 1
            FROM assets_suppliers
            WHERE asset_id = a.id AND third_country_transfer = 'YES'
        ) THEN TRUE
        ELSE FALSE
        END AS has_third_country_transfer,
    (SELECT COUNT(rel.id) FROM relations rel WHERE (rel.relation_a_id = a.id OR rel.relation_b_id = a.id) AND (rel.relation_a_type = 'REGISTER' OR rel.relation_b_type = 'REGISTER')) as registers
FROM assets a
    LEFT JOIN suppliers s on s.id = a.supplier_id
    LEFT JOIN properties ON properties.entity_id = a.id and properties.prop_key = 'kitos_uuid'
    LEFT JOIN threat_assessments ta ON ta.id = (
            SELECT tb.id FROM threat_assessments tb
                JOIN relations r ON (r.relation_a_id = a.id AND r.relation_b_id=tb.id
                                         OR r.relation_b_id = a.id AND r.relation_a_id=tb.id)
                         ORDER BY r.id DESC LIMIT 1)
    LEFT JOIN assets_responsible_users_mapping ru ON ru.asset_id = a.id
    LEFT JOIN users u ON ru.user_uuid = u.uuid
WHERE a.deleted = false
GROUP BY a.id;


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
    (CASE WHEN t.assessment = 'GREEN' THEN 1
          WHEN t.assessment = 'LIGHT_GREEN' THEN 2
          WHEN t.assessment = 'YELLOW' THEN 3
          WHEN t.assessment = 'ORANGE' THEN 4
          WHEN t.assessment = 'RED' THEN 5
        END) as assessment_order,
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
    (CASE WHEN d.document_type = 'OTHER' THEN 1
          WHEN d.document_type = 'WORKFLOW' THEN 2
          WHEN d.document_type = 'DATA_PROCESSING_AGREEMENT' THEN 3
          WHEN d.document_type = 'CONTRACT' THEN 4
          WHEN d.document_type = 'CONTROL' THEN 5
          WHEN d.document_type = 'MANAGEMENT_REPORT' THEN 6
          WHEN d.document_type = 'PROCEDURE' THEN 7
          WHEN d.document_type = 'RISK_ASSESSMENT_REPORT' THEN 8
          WHEN d.document_type = 'SUPERVISORY_REPORT' THEN 9
          WHEN d.document_type = 'GUIDE' THEN 10
          END) as document_type_order,
    d.responsible_uuid,
    d.next_revision,
    d.status,
    (CASE WHEN d.status = 'NOT_STARTED' THEN 1
          WHEN d.status = 'IN_PROGRESS' THEN 2
          WHEN d.status = 'READY' THEN 3
          END) as status_order,
    d.localized_enums,
    GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value ASC SEPARATOR ',') as tags
FROM documents d
    LEFT JOIN relatable_tags rt on rt.relatable_id = d.id
    LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE d.deleted=false
GROUP BY d.id;
