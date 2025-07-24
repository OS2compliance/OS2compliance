CREATE TABLE register_kle_legal_reference
(
    register_id BIGINT NOT NULL,
    accession_number VARCHAR(50) NOT NULL,
    PRIMARY KEY (register_id, accession_number),
    CONSTRAINT fk_register_kle_legal_reference_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_kle_legal_reference_accession_number FOREIGN KEY (accession_number) REFERENCES kle_legal_reference (accession_number) ON DELETE CASCADE
);