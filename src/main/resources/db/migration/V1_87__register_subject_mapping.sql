CREATE TABLE register_kle_subject_mapping (
    register_id BIGINT(20) NOT NULL,
    subject_number VARCHAR(50) NOT NULL,
    PRIMARY KEY (register_id, subject_number),
    CONSTRAINT fk_register_subject_number_mapping_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_subject_number_mapping_subject_number FOREIGN KEY (subject_number) REFERENCES kle_subject (subject_number) ON DELETE CASCADE
)  collate = utf8mb4_danish_ci;;