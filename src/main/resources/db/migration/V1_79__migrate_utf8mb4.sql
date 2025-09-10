ALTER TABLE mail_log
    CONVERT TO CHARACTER SET utf8mb4
        COLLATE utf8mb4_danish_ci;
-- 1) Drop foreign keys (child → parent rækkefølge)
ALTER TABLE register_kle_legal_reference DROP FOREIGN KEY fk_register_kle_legal_reference_register_id;
ALTER TABLE register_kle_legal_reference DROP FOREIGN KEY fk_register_kle_legal_reference_accession_number;

ALTER TABLE register_kle_group DROP FOREIGN KEY fk_register_kle_group_register_id;
ALTER TABLE register_kle_group DROP FOREIGN KEY fk_register_kle_group_kle_group_number;

ALTER TABLE register_kle_main_group DROP FOREIGN KEY fk_register_kle_main_group_register_id;
ALTER TABLE register_kle_main_group DROP FOREIGN KEY fk_register_kle_main_group_kle_main_group_number;

ALTER TABLE kle_subject_legal_reference DROP FOREIGN KEY FK_kle_subject_legal_reference_kle_subject;
ALTER TABLE kle_subject_legal_reference DROP FOREIGN KEY FK_kle_subject_legal_reference_kle_legal_reference;

ALTER TABLE kle_group_legal_reference DROP FOREIGN KEY FK_kle_group_legal_reference_group_number;
ALTER TABLE kle_group_legal_reference DROP FOREIGN KEY FK_le_group_legal_reference_accession_number;

ALTER TABLE kle_subject DROP FOREIGN KEY FK_kle_subject_group_number;

ALTER TABLE kle_group DROP FOREIGN KEY FK_kle_group_main_group_number;

-- 2) Sikr at datatyper matcher (register_id skal være BIGINT signed ligesom registers.id)
ALTER TABLE register_kle_main_group  MODIFY register_id BIGINT NOT NULL;
ALTER TABLE register_kle_group       MODIFY register_id BIGINT NOT NULL;
ALTER TABLE register_kle_legal_reference MODIFY register_id BIGINT NOT NULL;

-- 3) Konverter tabeller til utf8mb4_danish_ci
ALTER TABLE kle_legal_reference         CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE kle_main_group              CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE kle_group                   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE kle_subject                 CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE kle_group_legal_reference   CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE kle_subject_legal_reference CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE register_kle_main_group     CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE register_kle_group          CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;
ALTER TABLE register_kle_legal_reference CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci;

-- 4) Genskab foreign keys
ALTER TABLE kle_group
    ADD CONSTRAINT FK_kle_group_main_group_number
        FOREIGN KEY (main_group_number)
            REFERENCES kle_main_group (main_group_number)
            ON DELETE CASCADE;

ALTER TABLE kle_subject
    ADD CONSTRAINT FK_kle_subject_group_number
        FOREIGN KEY (group_number)
            REFERENCES kle_group (group_number)
            ON DELETE CASCADE;

ALTER TABLE kle_group_legal_reference
    ADD CONSTRAINT FK_kle_group_legal_reference_group_number
        FOREIGN KEY (group_number)
            REFERENCES kle_group (group_number)
            ON DELETE CASCADE,
    ADD CONSTRAINT FK_le_group_legal_reference_accession_number
        FOREIGN KEY (accession_number)
            REFERENCES kle_legal_reference (accession_number)
            ON DELETE CASCADE;

ALTER TABLE kle_subject_legal_reference
    ADD CONSTRAINT FK_kle_subject_legal_reference_kle_subject
        FOREIGN KEY (subject_number)
            REFERENCES kle_subject (subject_number)
            ON DELETE CASCADE,
    ADD CONSTRAINT FK_kle_subject_legal_reference_kle_legal_reference
        FOREIGN KEY (accession_number)
            REFERENCES kle_legal_reference (accession_number)
            ON DELETE CASCADE;

ALTER TABLE register_kle_main_group
    ADD CONSTRAINT fk_register_kle_main_group_register_id
        FOREIGN KEY (register_id)
            REFERENCES registers (id)
            ON DELETE CASCADE,
    ADD CONSTRAINT fk_register_kle_main_group_kle_main_group_number
        FOREIGN KEY (kle_main_group_number)
            REFERENCES kle_main_group (main_group_number)
            ON DELETE CASCADE;

ALTER TABLE register_kle_group
    ADD CONSTRAINT fk_register_kle_group_register_id
        FOREIGN KEY (register_id)
            REFERENCES registers (id)
            ON DELETE CASCADE,
    ADD CONSTRAINT fk_register_kle_group_kle_group_number
        FOREIGN KEY (kle_group_number)
            REFERENCES kle_group (group_number)
            ON DELETE CASCADE;

ALTER TABLE register_kle_legal_reference
    ADD CONSTRAINT fk_register_kle_legal_reference_register_id
        FOREIGN KEY (register_id)
            REFERENCES registers (id)
            ON DELETE CASCADE,
    ADD CONSTRAINT fk_register_kle_legal_reference_accession_number
        FOREIGN KEY (accession_number)
            REFERENCES kle_legal_reference (accession_number)
            ON DELETE CASCADE;
