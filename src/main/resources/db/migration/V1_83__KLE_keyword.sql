ALTER TABLE kle_subject
    MODIFY group_number VARCHAR(50) NULL;
ALTER TABLE kle_group
    MODIFY main_group_number VARCHAR(50) NULL;

CREATE TABLE kle_keyword
(
    hashed_id         VARCHAR(64) PRIMARY KEY,
    text              TEXT,
    handlingsfacet_nr VARCHAR(100)
) COLLATE = utf8mb4_danish_ci;;

CREATE INDEX idx_kle_keyword_text ON kle_keyword (text);

CREATE TABLE kle_group_keyword
(
    group_number VARCHAR(50),
    hashed_id    VARCHAR(64),
    PRIMARY KEY (group_number, hashed_id),
    CONSTRAINT FK_kle_group_kle_keyword_group_number FOREIGN KEY (group_number) REFERENCES kle_group (group_number) ON DELETE CASCADE,
    CONSTRAINT FK_le_group_kle_keyword_hashed_id FOREIGN KEY (hashed_id) REFERENCES kle_keyword (hashed_id) ON DELETE CASCADE
) COLLATE = utf8mb4_danish_ci;;

CREATE TABLE kle_subject_keyword
(
    subject_number VARCHAR(50),
    hashed_id      VARCHAR(64),
    PRIMARY KEY (subject_number, hashed_id),
    CONSTRAINT FK_kle_subject_kle_keyword_kle_subject FOREIGN KEY (subject_number) REFERENCES kle_subject (subject_number) ON DELETE CASCADE,
    CONSTRAINT FK_kle_subject_kle_keyword_hashed_id FOREIGN KEY (hashed_id) REFERENCES kle_keyword (hashed_id) ON DELETE CASCADE
) COLLATE = utf8mb4_danish_ci;;


