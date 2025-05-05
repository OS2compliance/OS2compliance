ALTER TABLE dpia
    ADD COLUMN created_at datetime(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN created_by varchar(255) NOT NULL DEFAULT 'Ukendt',
    ADD COLUMN name varchar(768) NOT NULL DEFAULT 'Konsekvensanalyse',
    ADD COLUMN updated_at datetime(6) null,
    ADD COLUMN updated_by varchar(255) null,
    ADD COLUMN relation_type varchar(30) not null DEFAULT 'DPIA',
    ADD COLUMN deleted bit default b'0' NOT NULL,
    ADD COLUMN localized_enums varchar(255) null,
    ADD COLUMN version int NOT NULL DEFAULT 0;

CREATE OR REPLACE
VIEW view_gridjs_dpia AS
SELECT
    d.id,
    a.name AS asset_name,
    d.updated_at,
    (SELECT COUNT(r.id) FROM relations r WHERE (r.relation_a_id = d.id OR r.relation_b_id = d.id) AND (r.relation_a_type = 'TASK' OR r.relation_b_type = 'TASK')) AS task_count
FROM
    dpia d
LEFT JOIN assets a ON a.id = d.asset_id
WHERE d.deleted = false;

