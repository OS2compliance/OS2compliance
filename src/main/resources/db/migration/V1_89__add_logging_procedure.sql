ALTER TABLE data_processing
    ADD COLUMN logging_procedure VARCHAR(255) DEFAULT NULL,
ADD COLUMN logging_procedure_link VARCHAR(255) DEFAULT NULL;