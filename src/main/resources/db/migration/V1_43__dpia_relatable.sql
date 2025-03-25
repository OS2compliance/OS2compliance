ALTER TABLE dpia
    ADD COLUMN created_at datetime(6)  not null,
    ADD COLUMN created_by varchar(255) not null,
    ADD COLUMN name varchar(768) not null,
    ADD COLUMN updated_at datetime(6) null,
    ADD COLUMN updated_by varchar(255) null,
    ADD COLUMN relation_type varchar(30) not null,
    ADD COLUMN deleted bit default b'0' null,
    ADD COLUMN localized_enums varchar(255) null;

CREATE OR REPLACE
VIEW view_gridjs_assessments AS
SELECT
    d.id,
    a.name AS asset_name,
    d.updated_at,
    (SELECT COUNT(r.id) FROM relations r WHERE (r.relation_a_id = d.id OR r.relation_b_id = d.id) AND (r.relation_a_type = 'TASK' OR r.relation_b_type = 'TASK')) AS task_count
FROM
    dpia d
LEFT JOIN assets a ON a.id = d.asset_id
WHERE d.deleted = false;