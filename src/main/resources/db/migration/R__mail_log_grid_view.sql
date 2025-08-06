CREATE OR REPLACE VIEW view_gridjs_dpia AS
SELECT ml.id            AS id,
       ml.sent_at       AS sent_at,
       ml.receiver      AS receiver,
       ml.subject       AS subject,
       ml.template_type AS type
FROM mail_log ml
ORDER BY ml.sentAt DESC