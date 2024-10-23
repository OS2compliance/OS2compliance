
ALTER TABLE incident_field_responses
    ADD COLUMN incident_field_id  BIGINT NULL;
ALTER TABLE incident_field_responses
    DROP COLUMN index_column_name;

ALTER TABLE incident_field_responses
    ADD CONSTRAINT FK_INCIDENT_FIELD_RESPONSES_ON_INCIDENT_FIELD FOREIGN KEY (incident_field_id)
        REFERENCES incident_fields (id) ON DELETE SET NULL;