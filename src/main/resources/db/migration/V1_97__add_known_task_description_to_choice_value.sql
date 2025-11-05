ALTER TABLE tasks ADD COLUMN task_description_template BIGINT;

-- Add foreign key constraint
ALTER TABLE tasks ADD CONSTRAINT fk_task_description_template
    FOREIGN KEY (task_description_template) REFERENCES choice_values(id);