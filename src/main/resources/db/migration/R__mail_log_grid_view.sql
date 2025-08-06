CREATE OR REPLACE VIEW view_gridjs_dpia AS
SELECT ml.id            AS id,
       ml.sentAt        AS sentAt,
       ml.receiver      AS receiver,
       ml.subject       AS subject,
       ml.template_type AS templateType
FROM mail_log ml
ORDER BY ml.sentAt DESC