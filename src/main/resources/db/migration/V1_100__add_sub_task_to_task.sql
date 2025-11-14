CREATE TABLE sub_tasks
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id     BIGINT,
    name        VARCHAR(255),
    completed   BOOLEAN NOT NULL DEFAULT FALSE
) collate = utf8mb4_danish_ci;
