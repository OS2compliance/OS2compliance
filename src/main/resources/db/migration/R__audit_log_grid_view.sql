CREATE OR REPLACE VIEW view_gridjs_audit_log AS
SELECT al.id                 AS id,
       al.created_timestamp  AS created_timestamp,
       al.performer_uuid     AS performer_uuid,
       al.performer_name     AS performer_name,
       al.entity_id          AS entity_id,
       al.entity_type        AS entity_type,
       al.entity_name        AS entity_name,
       al.revision           AS revision,
       al.description         AS description
FROM auditlog al
