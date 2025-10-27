INSERT INTO choice_lists (identifier, name, multi_select, customizable)
VALUES ('task-description-template', 'Opgavebeskrivelses-skabelon', 0, 1);

ALTER TABLE tasks ADD COLUMN task_description_template BIGINT;

-- Add foreign key constraint
ALTER TABLE tasks ADD CONSTRAINT fk_task_description_template
    FOREIGN KEY (task_description_template) REFERENCES choice_values(id);