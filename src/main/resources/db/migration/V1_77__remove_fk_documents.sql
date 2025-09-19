ALTER TABLE task_logs
    DROP FOREIGN KEY fk_task_logs_document_id;

ALTER TABLE task_logs
    ADD CONSTRAINT fk_task_logs_document_id
        FOREIGN KEY (document_id)
            REFERENCES documents (id)
            ON DELETE SET NULL;
