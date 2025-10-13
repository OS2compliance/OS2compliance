ALTER TABLE data_processing
    ADD COLUMN management_procedure VARCHAR(255) DEFAULT NULL,
ADD COLUMN user_management_procedure_link VARCHAR(255) DEFAULT NULL;