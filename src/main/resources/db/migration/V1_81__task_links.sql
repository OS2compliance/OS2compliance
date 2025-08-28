CREATE TABLE task_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    task_id BIGINT NOT NULL,
    CONSTRAINT fk_tasks_links_task_id FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;

INSERT INTO task_links (url, task_id)
SELECT link, id FROM tasks
WHERE link IS NOT NULL AND TRIM(link) != '';

ALTER TABLE tasks DROP COLUMN link;