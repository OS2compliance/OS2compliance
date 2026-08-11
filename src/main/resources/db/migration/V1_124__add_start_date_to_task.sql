ALTER TABLE tasks ADD COLUMN start_date DATE NOT NULL DEFAULT (CURRENT_DATE);
UPDATE tasks SET start_date = DATE(created_at) WHERE start_date IS NULL;

ALTER TABLE tasks_aud ADD COLUMN start_date DATE NULL;