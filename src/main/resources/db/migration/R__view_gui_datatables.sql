CREATE OR REPLACE VIEW view_gridjs_suppliers AS
SELECT s.id,
       TRIM(s.name)                                                         AS name,
       (SELECT COUNT(1) FROM assets a WHERE a.supplier_id = s.id)           AS solution_count,
       s.updated_at                                                         AS updated,
       s.status,
       s.localized_enums,
       (SELECT MAX(ao.creation_date)
        FROM assets a
                 LEFT JOIN assets_oversight ao ON ao.asset_id = a.id
        WHERE a.supplier_id = s.id)                                         AS last_oversight_date,
       prop.prop_value                                                      AS kitos_uuid,
       GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',') AS tag_names,
       GROUP_CONCAT(COALESCE(tg.id, '') ORDER BY tg.value SEPARATOR ',')    AS tag_ids,
       s.responsible_uuid
FROM suppliers s
         LEFT JOIN properties prop ON prop.entity_id = s.id AND prop.prop_key = 'kitos_uuid'
         LEFT JOIN supplier_tag rt ON rt.supplier_id = s.id
         LEFT JOIN tags tg ON rt.tag_id = tg.id
WHERE s.deleted = false
GROUP BY s.id;

CREATE OR REPLACE VIEW view_gridjs_tasks AS
SELECT t.id,
       t.name,
       t.task_type,
       GROUP_CONCAT(DISTINCT tru.user_uuid SEPARATOR ',') as responsible_uuid,
       GROUP_CONCAT(DISTINCT u.name SEPARATOR ', ') as responsible_names,
       t.responsible_ou_uuid,
       t.next_deadline,
       t.repetition,
       t.include_in_report,
       t.created_at,
       (CASE
            WHEN t.repetition = 'NONE' THEN 10
            WHEN t.repetition = 'MONTHLY' THEN 2
            WHEN t.repetition = 'QUARTERLY' THEN 3
            WHEN t.repetition = 'HALF_YEARLY' THEN 4
            WHEN t.repetition = 'YEARLY' THEN 5
            WHEN t.repetition = 'EVERY_SECOND_YEAR' THEN 6
            WHEN t.repetition = 'EVERY_THIRD_YEAR' THEN 7
           END)                                                                        as repetition_order,
       cv_result.caption                                                               as result,
       cv_result.id                                                                    as task_result_order,
       `ts`.`id` is not null and (`t`.`task_type` = 'TASK' or `t`.`repetition` = 'NONE') as `completed`,
       ts.completed                                                                    as last_completion_date,
       concat(COALESCE(t.localized_enums, ''), ' ', COALESCE(ts.localized_enums, ' ')) as localized_enums,
       GROUP_CONCAT(DISTINCT COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',') AS tag_names,
       GROUP_CONCAT(DISTINCT COALESCE(tg.id, '')   ORDER BY tg.value SEPARATOR ',') AS tag_ids
FROM tasks t
    LEFT JOIN task_responsible_users tru ON tru.task_id = t.id
    LEFT JOIN users u ON u.uuid = tru.user_uuid
    LEFT JOIN task_logs ts on ts.task_id = t.id
    LEFT JOIN choice_values cv_result ON cv_result.id = ts.task_result
    LEFT JOIN task_tag rt on rt.task_id = t.id
    LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE t.deleted = false
  AND (ts.id IS NULL OR ts.id = (SELECT MAX(id) FROM task_logs WHERE task_id = t.id))
GROUP BY t.id;

CREATE OR REPLACE VIEW view_gridjs_registers AS
SELECT r.id,
       r.name,
       GROUP_CONCAT(DISTINCT u.name SEPARATOR ', ')                                   as responsible_user_names,
       GROUP_CONCAT(DISTINCT u.uuid SEPARATOR ',')                                    as responsible_user_uuids,
       GROUP_CONCAT(DISTINCT cru.uuid SEPARATOR ',')                                  as custom_responsible_user_uuids,
       GROUP_CONCAT(DISTINCT ou.name SEPARATOR ', ')                                  as responsible_ou_names,
       GROUP_CONCAT(DISTINCT d.name SEPARATOR ', ')                                   as department_names,
       r.updated_at,
       ca.assessment                                                                  as consequence,
       (CASE
            WHEN ca.assessment = 'GREEN' THEN 1
            WHEN ca.assessment = 'LIGHT_GREEN' THEN 2
            WHEN ca.assessment = 'YELLOW' THEN 3
            WHEN ca.assessment = 'ORANGE' THEN 4
            WHEN ca.assessment = 'RED' THEN 5
           END)                                                                       as consequence_order,
       ta.assessment                                                                  as risk,
       (CASE
            WHEN ta.assessment = 'GREEN' THEN 1
            WHEN ta.assessment = 'LIGHT_GREEN' THEN 2
            WHEN ta.assessment = 'YELLOW' THEN 3
            WHEN ta.assessment = 'ORANGE' THEN 4
            WHEN ta.assessment = 'RED' THEN 5
           END)                                                                       as risk_order,
       concat(COALESCE(r.localized_enums, ''), ' ', COALESCE(ta.localized_enums, '')) as localized_enums,
       cv_status.caption                                                              as status,
       cv_status.id                                                                   as status_order,
       (SELECT COUNT(rel.id)
        FROM relations rel
        WHERE (rel.relation_a_id = r.id OR rel.relation_b_id = r.id)
          AND (rel.relation_a_type = 'ASSET' OR rel.relation_b_type = 'ASSET'))       AS asset_count,
       pr.prop_value                                                                  as asset_assessment,
       (CASE
            WHEN pr.prop_value = 'GREEN' THEN 1
            WHEN pr.prop_value = 'LIGHT_GREEN' THEN 2
            WHEN pr.prop_value = 'YELLOW' THEN 3
            WHEN pr.prop_value = 'ORANGE' THEN 4
            WHEN pr.prop_value = 'RED' THEN 5
           END)                                                                       as asset_assessment_order,
       GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',')           AS tag_names,
       GROUP_CONCAT(COALESCE(tg.id, '') ORDER BY tg.value SEPARATOR ',')              AS tag_ids,
       ta_calcs.avg_probability,
       ta_calcs.avg_consequence_overall,
       ta_calcs.avg_consequence_confidentiality_registered,
       ta_calcs.avg_consequence_confidentiality_organisation,
       ta_calcs.avg_consequence_confidentiality_society,
       ta_calcs.avg_consequence_integrity_registered,
       ta_calcs.avg_consequence_integrity_organisation,
       ta_calcs.avg_consequence_integrity_society,
       ta_calcs.avg_consequence_availability_registered,
       ta_calcs.avg_consequence_availability_organisation,
       ta_calcs.avg_consequence_availability_society,
       ta_calcs.avg_consequence_authenticity_society,
       threat_types.threat_type_list,
       threat_catalogs.catalog_list,
       (CASE
           WHEN ta_calcs.avg_probability IS NOT NULL AND ta_calcs.avg_consequence_overall IS NOT NULL
           THEN ROUND(ta_calcs.avg_probability * ta_calcs.avg_consequence_overall, 2)
           ELSE NULL
       END) as risk_score
FROM registers r
         LEFT JOIN choice_values cv_status ON cv_status.id = r.status
         LEFT JOIN consequence_assessments ca on ca.register_id = r.id
         LEFT JOIN threat_assessments ta ON ta.id = (
             SELECT tb.id
             FROM threat_assessments tb
             JOIN relations rel ON (
                 rel.relation_a_id = r.id AND rel.relation_b_id = tb.id AND rel.relation_b_type = 'THREAT_ASSESSMENT'
                 OR rel.relation_b_id = r.id AND rel.relation_a_id = tb.id AND rel.relation_a_type = 'THREAT_ASSESSMENT'
             )
             WHERE tb.deleted = false
               AND tb.hidden = false
             ORDER BY tb.created_at DESC
             LIMIT 1
         )
         LEFT JOIN (
             SELECT
                 tar.threat_assessment_id,
                 AVG(tar.probability) as avg_probability,
                 SUM(
                     COALESCE(tar.confidentiality_registered, 0) +
                     COALESCE(tar.confidentiality_organisation, 0) +
                     COALESCE(tar.confidentiality_society, 0) +
                     COALESCE(tar.integrity_registered, 0) +
                     COALESCE(tar.integrity_organisation, 0) +
                     COALESCE(tar.integrity_society, 0) +
                     COALESCE(tar.availability_registered, 0) +
                     COALESCE(tar.availability_organisation, 0) +
                     COALESCE(tar.availability_society, 0) +
                     COALESCE(tar.authenticity_society, 0)
                 ) / NULLIF(SUM(
                     (tar.confidentiality_registered IS NOT NULL) +
                     (tar.confidentiality_organisation IS NOT NULL) +
                     (tar.confidentiality_society IS NOT NULL) +
                     (tar.integrity_registered IS NOT NULL) +
                     (tar.integrity_organisation IS NOT NULL) +
                     (tar.integrity_society IS NOT NULL) +
                     (tar.availability_registered IS NOT NULL) +
                     (tar.availability_organisation IS NOT NULL) +
                     (tar.availability_society IS NOT NULL) +
                     (tar.authenticity_society IS NOT NULL)
                 ), 0) as avg_consequence_overall,
                 COALESCE(AVG(tar.confidentiality_registered), 0) as avg_consequence_confidentiality_registered,
                 COALESCE(AVG(tar.confidentiality_organisation), 0) as avg_consequence_confidentiality_organisation,
                 COALESCE(AVG(tar.confidentiality_society), 0) as avg_consequence_confidentiality_society,
                 COALESCE(AVG(tar.integrity_registered), 0) as avg_consequence_integrity_registered,
                 COALESCE(AVG(tar.integrity_organisation), 0) as avg_consequence_integrity_organisation,
                 COALESCE(AVG(tar.integrity_society), 0) as avg_consequence_integrity_society,
                 COALESCE(AVG(tar.availability_registered), 0) as avg_consequence_availability_registered,
                 COALESCE(AVG(tar.availability_organisation), 0) as avg_consequence_availability_organisation,
                 COALESCE(AVG(tar.availability_society), 0) as avg_consequence_availability_society,
                 COALESCE(AVG(tar.authenticity_society), 0) as avg_consequence_authenticity_society
             FROM threat_assessment_responses tar
             WHERE tar.not_relevant = false
             GROUP BY tar.threat_assessment_id
         ) ta_calcs ON ta_calcs.threat_assessment_id = ta.id
         LEFT JOIN (
             SELECT
                 tar.threat_assessment_id,
                 GROUP_CONCAT(DISTINCT
                     COALESCE(tct.threat_type, ct.threat_type)
                     ORDER BY COALESCE(tct.threat_type, ct.threat_type)
                     SEPARATOR ', '
                 ) as threat_type_list
             FROM threat_assessment_responses tar
             LEFT JOIN threat_catalog_threats tct ON tar.threat_catalog_threat_id = tct.identifier
             LEFT JOIN custom_threats ct ON tar.custom_threat_id = ct.id
             WHERE tar.not_relevant = false
               AND (tct.threat_type IS NOT NULL OR ct.threat_type IS NOT NULL)
             GROUP BY tar.threat_assessment_id
         ) threat_types ON threat_types.threat_assessment_id = ta.id
         LEFT JOIN (
             SELECT
                 tac.threat_assessment_id,
                 GROUP_CONCAT(DISTINCT tc.name ORDER BY tc.name SEPARATOR ', ') as catalog_list
             FROM threat_assessment_catalogs tac
             JOIN threat_catalogs tc ON tac.threat_catalog_identifier = tc.identifier
             WHERE tc.deleted = false
             GROUP BY tac.threat_assessment_id
         ) threat_catalogs ON threat_catalogs.threat_assessment_id = ta.id
         LEFT JOIN registers_responsible_users_mapping rum ON rum.register_id = r.id
         LEFT JOIN users u ON rum.user_uuid = u.uuid
         LEFT JOIN register_custom_responsible_user_mapping crum ON crum.register_id = r.id
         LEFT JOIN users cru ON crum.user_uuid = cru.uuid
         LEFT JOIN registers_responsible_ous_mapping roum ON roum.register_id = r.id
         LEFT JOIN ous ou ON roum.ou_uuid = ou.uuid
         LEFT JOIN registers_departments_mapping rdm ON rdm.register_id = r.id
         LEFT JOIN ous d ON rdm.ou_uuid = d.uuid
         LEFT JOIN properties pr on pr.entity_id = r.id and pr.prop_key = 'asset_assessment'
         LEFT JOIN register_tag rt on rt.register_id = r.id
         LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE r.deleted = false
GROUP BY r.id;


CREATE OR REPLACE VIEW view_gridjs_assets AS
SELECT a.id,
       a.name,
       s.name                                                               as supplier,
       cv.caption                                                           as asset_type,
       GROUP_CONCAT(DISTINCT u.name SEPARATOR ',')                          as responsible_user_names,
       GROUP_CONCAT(DISTINCT u.uuid SEPARATOR ',')                          as responsible_user_uuids,
       GROUP_CONCAT(DISTINCT mu.uuid SEPARATOR ',')                         as manager_uuids,
       GROUP_CONCAT(DISTINCT mu.name SEPARATOR ',')                         as manager_user_names,
       a.updated_at,
       a.asset_status,
       (CASE
            WHEN a.asset_status = 'NOT_STARTED' THEN 1
            WHEN a.asset_status = 'ON_GOING' THEN 2
            WHEN a.asset_status = 'READY' THEN 3
           END
           )                                                                as asset_status_order,
       a.asset_category,
       a.active                                                             AS active,
       (CASE
            WHEN a.asset_category = 'GREEN' THEN 1
            WHEN a.asset_category = 'YELLOW' THEN 2
            WHEN a.asset_category = 'RED' THEN 3
           END
           )                                                                as asset_category_order,
       ta.assessment,
       (CASE
            WHEN ta.assessment = 'GREEN' THEN 1
            WHEN ta.assessment = 'LIGHT_GREEN' THEN 2
            WHEN ta.assessment = 'YELLOW' THEN 3
            WHEN ta.assessment = 'ORANGE' THEN 4
            WHEN ta.assessment = 'RED' THEN 5
           END
           )                                                                as assessment_order,
       concat(COALESCE(a.localized_enums, ''
              ), ' ', COALESCE(ta.localized_enums, ''
                      )
       )                                                                    as localized_enums,
       IF(properties.prop_value IS null, 0, 1)                              AS kitos,
       IF(old_kitos_prop.prop_value IS NULL, 0, 1)                          AS old_kitos,
       MAX(ao.creation_date)                                                AS last_oversight_date,
       GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',') AS tag_names,
       GROUP_CONCAT(COALESCE(tg.id, '') ORDER BY tg.value SEPARATOR ',')    AS tag_ids,
       CASE
           WHEN EXISTS (SELECT 1
                        FROM assets_suppliers
                        WHERE asset_id = a.id
                          AND third_country_transfer = 'YES') THEN TRUE
           ELSE FALSE
       END                                                                  AS has_third_country_transfer,
       (SELECT COUNT(rel.id)
        FROM relations rel
        WHERE (rel.relation_a_id = a.id OR rel.relation_b_id = a.id)
          AND (rel.relation_a_type = 'REGISTER' OR rel.relation_b_type = 'REGISTER')) as registers,
       ta_calcs.avg_probability,
       ta_calcs.avg_consequence_overall,
       ta_calcs.avg_consequence_confidentiality_registered,
       ta_calcs.avg_consequence_confidentiality_organisation,
       ta_calcs.avg_consequence_confidentiality_society,
       ta_calcs.avg_consequence_integrity_registered,
       ta_calcs.avg_consequence_integrity_organisation,
       ta_calcs.avg_consequence_integrity_society,
       ta_calcs.avg_consequence_availability_registered,
       ta_calcs.avg_consequence_availability_organisation,
       ta_calcs.avg_consequence_availability_society,
       ta_calcs.avg_consequence_authenticity_society,
       threat_types.threat_type_list,
       threat_catalogs.catalog_list,
       (CASE
           WHEN ta_calcs.avg_probability IS NOT NULL AND ta_calcs.avg_consequence_overall IS NOT NULL
           THEN ROUND(ta_calcs.avg_probability * ta_calcs.avg_consequence_overall, 2)
           ELSE NULL
       END) as risk_score
FROM assets a
         LEFT JOIN suppliers s on s.id = a.supplier_id
         LEFT JOIN properties ON properties.entity_id = a.id and properties.prop_key = 'kitos_uuid'
         LEFT JOIN properties old_kitos_prop ON old_kitos_prop.entity_id = a.id AND old_kitos_prop.prop_key = 'old_kitos_usage_uuid'
         LEFT JOIN threat_assessments ta ON ta.id = (
             SELECT tb.id
             FROM threat_assessments tb
             JOIN relations r ON (
                 r.relation_a_id = a.id AND r.relation_b_id = tb.id AND r.relation_b_type = 'THREAT_ASSESSMENT'
                 OR r.relation_b_id = a.id AND r.relation_a_id = tb.id AND r.relation_a_type = 'THREAT_ASSESSMENT'
             )
             WHERE tb.deleted = false
               AND tb.hidden = false
             ORDER BY tb.created_at DESC
             LIMIT 1
         )
         LEFT JOIN (
             SELECT
                 tar.threat_assessment_id,
                 AVG(tar.probability) as avg_probability,
                 SUM(
                     COALESCE(tar.confidentiality_registered, 0) +
                     COALESCE(tar.confidentiality_organisation, 0) +
                     COALESCE(tar.confidentiality_society, 0) +
                     COALESCE(tar.integrity_registered, 0) +
                     COALESCE(tar.integrity_organisation, 0) +
                     COALESCE(tar.integrity_society, 0) +
                     COALESCE(tar.availability_registered, 0) +
                     COALESCE(tar.availability_organisation, 0) +
                     COALESCE(tar.availability_society, 0) +
                     COALESCE(tar.authenticity_society, 0)
                 ) / NULLIF(SUM(
                     (tar.confidentiality_registered IS NOT NULL) +
                     (tar.confidentiality_organisation IS NOT NULL) +
                     (tar.confidentiality_society IS NOT NULL) +
                     (tar.integrity_registered IS NOT NULL) +
                     (tar.integrity_organisation IS NOT NULL) +
                     (tar.integrity_society IS NOT NULL) +
                     (tar.availability_registered IS NOT NULL) +
                     (tar.availability_organisation IS NOT NULL) +
                     (tar.availability_society IS NOT NULL) +
                     (tar.authenticity_society IS NOT NULL)
                 ), 0) as avg_consequence_overall,
                 COALESCE(AVG(tar.confidentiality_registered), 0) as avg_consequence_confidentiality_registered,
                 COALESCE(AVG(tar.confidentiality_organisation), 0) as avg_consequence_confidentiality_organisation,
                 COALESCE(AVG(tar.confidentiality_society), 0) as avg_consequence_confidentiality_society,
                 COALESCE(AVG(tar.integrity_registered), 0) as avg_consequence_integrity_registered,
                 COALESCE(AVG(tar.integrity_organisation), 0) as avg_consequence_integrity_organisation,
                 COALESCE(AVG(tar.integrity_society), 0) as avg_consequence_integrity_society,
                 COALESCE(AVG(tar.availability_registered), 0) as avg_consequence_availability_registered,
                 COALESCE(AVG(tar.availability_organisation), 0) as avg_consequence_availability_organisation,
                 COALESCE(AVG(tar.availability_society), 0) as avg_consequence_availability_society,
                 COALESCE(AVG(tar.authenticity_society), 0) as avg_consequence_authenticity_society
             FROM threat_assessment_responses tar
             WHERE tar.not_relevant = false
             GROUP BY tar.threat_assessment_id
         ) ta_calcs ON ta_calcs.threat_assessment_id = ta.id
         LEFT JOIN (
             SELECT
                 tar.threat_assessment_id,
                 GROUP_CONCAT(DISTINCT
                     COALESCE(tct.threat_type, ct.threat_type)
                     ORDER BY COALESCE(tct.threat_type, ct.threat_type)
                     SEPARATOR ', '
                 ) as threat_type_list
             FROM threat_assessment_responses tar
             LEFT JOIN threat_catalog_threats tct ON tar.threat_catalog_threat_id = tct.identifier
             LEFT JOIN custom_threats ct ON tar.custom_threat_id = ct.id
             WHERE tar.not_relevant = false
               AND (tct.threat_type IS NOT NULL OR ct.threat_type IS NOT NULL)
             GROUP BY tar.threat_assessment_id
         ) threat_types ON threat_types.threat_assessment_id = ta.id
         LEFT JOIN (
             SELECT
                 tac.threat_assessment_id,
                 GROUP_CONCAT(DISTINCT tc.name ORDER BY tc.name SEPARATOR ', ') as catalog_list
             FROM threat_assessment_catalogs tac
             JOIN threat_catalogs tc ON tac.threat_catalog_identifier = tc.identifier
             WHERE tc.deleted = false
             GROUP BY tac.threat_assessment_id
         ) threat_catalogs ON threat_catalogs.threat_assessment_id = ta.id
         LEFT JOIN assets_responsible_users_mapping ru ON ru.asset_id = a.id
         LEFT JOIN users u ON ru.user_uuid = u.uuid
         LEFT JOIN choice_values cv ON a.asset_type = cv.id
         LEFT JOIN assets_users_mapping aum ON aum.asset_id = a.id
         LEFT JOIN users mu ON aum.user_uuid = mu.uuid
         LEFT JOIN assets_oversight ao ON ao.asset_id = a.id
         LEFT JOIN asset_tag rt on rt.asset_id = a.id
         LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE a.deleted = false
GROUP BY a.id;


CREATE OR REPLACE VIEW view_gridjs_assessments AS
SELECT t.id,
       TRIM(t.name)                                                                                                                                                  as name,
       t.responsible_uuid,
       t.responsible_ou_uuid,
       t.threat_assessment_type                                                                                                                                      as type,
       t.threat_assessment_report_user_uuid                                                                                                                          as signer_uuid,
       t.threat_assessment_report_approval_status,
       t.updated_at                                                                                                                                                  as date,
       t.assessment,
       t.hidden,
       t.localized_enums,
       (CASE
            WHEN t.assessment = 'GREEN' THEN 1
            WHEN t.assessment = 'LIGHT_GREEN' THEN 2
            WHEN t.assessment = 'YELLOW' THEN 3
            WHEN t.assessment = 'ORANGE' THEN 4
            WHEN t.assessment = 'RED' THEN 5
           END)                                                                                                                                                      as assessment_order,
       (SELECT COUNT(r.id) FROM relations r WHERE (r.relation_a_id = t.id OR r.relation_b_id = t.id) AND (r.relation_a_type = 'TASK' OR r.relation_b_type = 'TASK')) AS tasks,
       (SELECT COUNT(r.id)
        FROM relations r
        JOIN tasks task ON (
            (r.relation_a_id = task.id AND r.relation_a_type = 'TASK' AND r.relation_b_id = t.id) OR
            (r.relation_b_id = task.id AND r.relation_b_type = 'TASK' AND r.relation_a_id = t.id)
            )
        WHERE (SELECT CASE
                    WHEN EXISTS (SELECT 1 FROM task_logs tl WHERE tl.task_id = task.id) THEN 'COMPLETED'
                    WHEN task.next_deadline > CURRENT_TIMESTAMP() THEN 'FUTURE'
                    ELSE 'EXCEEDED'
                    END) = 'COMPLETED'
        ) AS completed_tasks,
       t.from_external_source,
       t.external_link,
       GROUP_CONCAT(DISTINCT
                    CASE
                        WHEN a.name IS NOT NULL THEN a.name
                        WHEN rgs.name IS NOT NULL THEN rgs.name
                        END
                    ORDER BY
                    CASE
                        WHEN a.name IS NOT NULL THEN a.name
                        WHEN rgs.name IS NOT NULL THEN rgs.name
                        END ASC
                    SEPARATOR '||'
       )                                                                                                                                                             AS related_assets_and_registers,
       (SELECT GROUP_CONCAT(DISTINCT tc.name ORDER BY tc.name ASC SEPARATOR ',')
        FROM threat_assessment_catalogs tac
                 LEFT JOIN threat_catalogs tc ON tac.threat_catalog_identifier = tc.identifier
        WHERE tac.threat_assessment_id = t.id
          AND tc.deleted = false)                                                                                                                                    AS threat_catalogs,
       (SELECT GROUP_CONCAT(DISTINCT tg.value ORDER BY tg.value SEPARATOR ',')
        FROM threat_assessment_tag rt LEFT JOIN tags tg ON rt.tag_id = tg.id
        WHERE rt.threat_assessment_id = t.id) AS tag_names,
       (SELECT GROUP_CONCAT(DISTINCT tg.id ORDER BY tg.value SEPARATOR ',')
        FROM threat_assessment_tag rt LEFT JOIN tags tg ON rt.tag_id = tg.id
        WHERE rt.threat_assessment_id = t.id) AS tag_ids
FROM threat_assessments t
         LEFT JOIN relations rel ON (
    (rel.relation_a_type = 'THREAT_ASSESSMENT' AND rel.relation_a_id = t.id)
        OR (rel.relation_b_type = 'THREAT_ASSESSMENT' AND rel.relation_b_id = t.id)
    )
         LEFT JOIN assets a ON (
    (rel.relation_a_type = 'ASSET' AND rel.relation_a_id = a.id AND rel.relation_b_type = 'THREAT_ASSESSMENT' AND rel.relation_b_id = t.id)
        OR (rel.relation_b_type = 'ASSET' AND rel.relation_b_id = a.id AND rel.relation_a_type = 'THREAT_ASSESSMENT' AND rel.relation_a_id = t.id)
    )
         LEFT JOIN registers rgs ON (
    (rel.relation_a_type = 'REGISTER' AND rel.relation_a_id = rgs.id AND rel.relation_b_type = 'THREAT_ASSESSMENT' AND rel.relation_b_id = t.id)
        OR (rel.relation_b_type = 'REGISTER' AND rel.relation_b_id = rgs.id AND rel.relation_a_type = 'THREAT_ASSESSMENT' AND rel.relation_a_id = t.id)
    )
WHERE t.deleted = false
GROUP BY t.id;

CREATE OR REPLACE VIEW view_gridjs_documents AS
SELECT d.id,
       d.name,
       cv_type.caption                                                              as document_type,
       cv_type.id                                                                   as document_type_order,
       d.responsible_uuid,
       d.next_revision,
       d.status,
       (CASE
            WHEN d.status = 'NOT_STARTED' THEN 1
            WHEN d.status = 'IN_PROGRESS' THEN 2
            WHEN d.status = 'READY' THEN 3
           END)                                                             as status_order,
       d.localized_enums,
       GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',') AS tag_names,
       GROUP_CONCAT(COALESCE(tg.id, '') ORDER BY tg.value SEPARATOR ',')    AS tag_ids
FROM documents d
         LEFT JOIN choice_values cv_type ON cv_type.id = d.document_type
         LEFT JOIN document_tag rt on rt.document_id = d.id
         LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE d.deleted = false
GROUP BY d.id;

CREATE OR REPLACE VIEW view_responsible_users AS
SELECT uuid,
       name,
       user_id,
       email,
       active,
       GROUP_CONCAT(DISTINCT id ORDER BY id SEPARATOR ',') AS responsible_relatable_ids
FROM (SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             t.id
      FROM users u
           LEFT JOIN task_responsible_users tru ON u.uuid = tru.user_uuid
           LEFT JOIN tasks t ON tru.task_id = t.id AND t.deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             d.id
      FROM users u
               LEFT JOIN documents d ON u.uuid = d.responsible_uuid and deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             s.id
      FROM users u
               LEFT JOIN standard_sections s ON u.uuid = s.responsible_user_uuid and deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             su.id
      FROM users u
               LEFT JOIN suppliers su ON u.uuid = su.responsible_uuid and deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             ta.id
      FROM users u
               LEFT JOIN threat_assessments ta ON u.uuid = ta.responsible_uuid and deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             r.id
      FROM users u
               LEFT JOIN registers_responsible_users_mapping rr ON u.uuid = rr.user_uuid
               LEFT JOIN registers r ON rr.register_id = r.id and deleted = 0

      UNION ALL

      SELECT u.uuid,
             u.name,
             u.user_id,
             u.email,
             u.active,
             a.id
      FROM users u
               LEFT JOIN assets_responsible_users_mapping ar ON u.uuid = ar.user_uuid
               LEFT JOIN assets a ON ar.asset_id = a.id and deleted = 0) AS combined_ids
GROUP BY uuid, name, user_id, email, active
HAVING responsible_relatable_ids IS NOT NULL
   AND responsible_relatable_ids <> '';

CREATE OR REPLACE VIEW view_gridjs_dbs_assets AS
SELECT a.id,
       a.name,
       a.last_sync,
       s.name                                               as supplier,
       GROUP_CONCAT(a2.id ORDER BY a2.id SEPARATOR ',')     AS assets_ids,
       GROUP_CONCAT(a2.name ORDER BY a2.name SEPARATOR ',') AS asset_names
FROM dbs_asset a
         LEFT JOIN dbs_supplier s on a.dbs_supplier_id = s.id
         LEFT JOIN relations r on ((r.relation_a_id = a.id OR r.relation_b_id = a.id) AND (r.relation_a_type = 'DBSASSET' OR r.relation_b_type = 'DBSASSET'))
         LEFT JOIN assets a2 on r.relation_a_id = a2.id OR r.relation_b_id = a2.id
WHERE a.deleted = false
GROUP BY a.id;

CREATE OR REPLACE VIEW view_gridjs_dbs_oversights AS
SELECT a.id,
       a.name,
       s.name                                                                                as supplier,
       s.id                                                                                  as supplier_id,
       cv_supervisory.caption                                                                as supervisory_model,
       GROUP_CONCAT(da.id ORDER BY da.id SEPARATOR ',')                                      AS dbs_assets,
       GROUP_CONCAT(da.name ORDER BY da.name SEPARATOR ',')                                  AS dbs_asset_names,
       a.oversight_responsible_uuid,
       latest_ao.creation_date                                                               as last_inspection,
       latest_ao.status                                                                      as last_inspection_status,
       IF(tl.id is null, t.id, null)                                                         AS outstanding_task_id,
       concat(COALESCE(a.localized_enums, ''), ' ', COALESCE(latest_ao.localized_enums, '')) as localized_enums
FROM assets a
         LEFT JOIN suppliers s on s.id = a.supplier_id
         LEFT JOIN (SELECT ao.*
                    FROM assets_oversight ao
                             INNER JOIN (SELECT asset_id, MAX(creation_date) as max_date
                                         FROM assets_oversight
                                         GROUP BY asset_id) ao_max ON ao.asset_id = ao_max.asset_id AND ao.creation_date = ao_max.max_date) latest_ao ON latest_ao.asset_id = a.id
         LEFT JOIN choice_values cv_supervisory ON cv_supervisory.id = latest_ao.supervision_model
         LEFT JOIN relations r on ((r.relation_a_id = a.id OR r.relation_b_id = a.id) AND (r.relation_a_type = 'DBSASSET' OR r.relation_b_type = 'DBSASSET'))
         LEFT JOIN dbs_asset da on r.relation_a_id = da.id OR r.relation_b_id = da.id
         LEFT JOIN relations r1 on ((r1.relation_a_id = da.id OR r1.relation_b_id = da.id) AND (r1.relation_a_type = 'TASK' OR r1.relation_b_type = 'TASK'))
         LEFT JOIN tasks t on r1.relation_a_id = t.id or r1.relation_b_id = t.id
         LEFT JOIN task_logs tl on tl.task_id = t.id
WHERE a.deleted = false
GROUP BY a.id;

CREATE OR REPLACE VIEW view_gridjs_dpia AS
SELECT d.id,
       d.name,
       (SELECT us.name FROM users us WHERE us.uuid = d.responsible_user_uuid)                                                                                        AS responsible_user_name,
       (SELECT us.uuid FROM users us WHERE us.uuid = d.responsible_user_uuid)                                                                                        AS responsible_user_uuid,
       (SELECT ou.name FROM ous ou WHERE ou.uuid = d.responsible_ou_uuid)                                                                                            AS responsible_ou_name,
       d.user_updated_date,
       (SELECT COUNT(r.id) FROM relations r WHERE (r.relation_a_id = d.id OR r.relation_b_id = d.id) AND (r.relation_a_type = 'TASK' OR r.relation_b_type = 'TASK')) AS task_count,
       (SELECT dr.dpia_report_approval_status FROM dpia_report dr WHERE dr.dpia_id = d.id order by dr.id desc limit 1)                                               AS report_approval_status,
       (SELECT sc.conclusion FROM dpia_screening sc WHERE sc.dpia_id = d.id)                                                                                         as screening_conclusion,
       d.from_external_source                                                                                                                                        as is_external,
       dr.report_approver_uuid                                                                                                                                       AS approver_uuid,
       GROUP_CONCAT(COALESCE(tg.value, '') ORDER BY tg.value SEPARATOR ',')                                                                                          AS tag_names,
       GROUP_CONCAT(COALESCE(tg.id, '') ORDER BY tg.value SEPARATOR ',')                                                                                             AS tag_ids
FROM dpia d
         LEFT JOIN dpia_report dr ON d.id = dr.dpia_id
         LEFT JOIN dpia_tag rt on rt.dpia_id = d.id
         LEFT JOIN tags tg on rt.tag_id = tg.id
WHERE d.deleted = false
GROUP BY d.id, d.name, d.responsible_user_uuid, d.responsible_ou_uuid,
         d.user_updated_date, d.from_external_source, dr.report_approver_uuid;