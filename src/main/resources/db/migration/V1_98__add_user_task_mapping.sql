CREATE TABLE task_responsible_users (
    task_id BIGINT NOT NULL,
    user_uuid VARCHAR(36) NOT NULL,
    PRIMARY KEY (task_id, user_uuid),
    FOREIGN KEY (task_id) REFERENCES tasks(id),
    FOREIGN KEY (user_uuid) REFERENCES users(uuid)
) COLLATE = utf8mb4_danish_ci;

INSERT INTO task_responsible_users (task_id, user_uuid)
SELECT id, responsible_uuid
FROM tasks
WHERE responsible_uuid IS NOT NULL;

ALTER TABLE tasks ADD COLUMN preserved_responsible_users VARCHAR(1000) DEFAULT NULL;