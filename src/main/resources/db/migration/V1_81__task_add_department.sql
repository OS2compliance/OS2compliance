ALTER TABLE tasks
ADD COLUMN department_uuid VARCHAR(255);

ALTER TABLE tasks
ADD CONSTRAINT fk_tasks_department_uuid
FOREIGN KEY (department_uuid) REFERENCES ous(uuid);