CREATE TABLE kle_legal_reference
(
    accession_number VARCHAR(50) PRIMARY KEY,
    paragraph        TEXT,
    url              VARCHAR(500),
    title            VARCHAR(500) NOT NULL,
    deleted          BOOLEAN DEFAULT FALSE
);

CREATE TABLE kle_main_group
(
    main_group_number VARCHAR(50) PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    instruction_text  TEXT,
    creation_date     DATE,
    last_update_date  DATE,
    uuid              VARCHAR(36)  NULL, -- At this point in time, uuid is not guaranteed present or unique
    deleted           BOOLEAN DEFAULT FALSE
);

CREATE TABLE kle_group
(
    group_number      VARCHAR(50) PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    instruction_text  TEXT,
    creation_date     DATE,
    last_update_date  DATE,
    uuid              VARCHAR(36)  NULL, -- At this point in time, uuid is not guaranteed present or unique,
    deleted           BOOLEAN DEFAULT FALSE,
    main_group_number VARCHAR(50)  NOT NULL,
    CONSTRAINT FK_kle_group_main_group_number FOREIGN KEY (main_group_number) REFERENCES kle_main_group (main_group_number) ON DELETE CASCADE
);

-- Table: kle_subject
CREATE TABLE kle_subject
(
    subject_number           VARCHAR(50) PRIMARY KEY,
    title                    VARCHAR(255) NOT NULL,
    instruction_text         TEXT,
    creation_date            DATE,
    last_update_date         DATE,
    preservation_code        VARCHAR(50),
    duration_before_deletion VARCHAR(50),
    uuid                     VARCHAR(36)  NULL, -- At this point in time, uuid is not guaranteed present or unique
    deleted                  BOOLEAN DEFAULT FALSE,
    group_number             VARCHAR(50)  NOT NULL,
    CONSTRAINT FK_kle_subject_group_number FOREIGN KEY (group_number) REFERENCES kle_group (group_number) ON DELETE CASCADE
);

CREATE TABLE kle_group_legal_reference
(
    group_number     VARCHAR(50),
    accession_number VARCHAR(50),
    PRIMARY KEY (group_number, accession_number),
    CONSTRAINT FK_kle_group_legal_reference_group_number FOREIGN KEY (group_number) REFERENCES kle_group (group_number) ON DELETE CASCADE,
    CONSTRAINT FK_le_group_legal_reference_accession_number FOREIGN KEY (accession_number) REFERENCES kle_legal_reference (accession_number) ON DELETE CASCADE
);

CREATE TABLE kle_subject_legal_reference
(
    subject_number   VARCHAR(50),
    accession_number VARCHAR(50),
    PRIMARY KEY (subject_number, accession_number),
    CONSTRAINT FK_kle_subject_legal_reference_kle_subject FOREIGN KEY (subject_number) REFERENCES kle_subject (subject_number) ON DELETE CASCADE,
    CONSTRAINT FK_kle_subject_legal_reference_kle_legal_reference FOREIGN KEY (accession_number) REFERENCES kle_legal_reference (accession_number) ON DELETE CASCADE
);

CREATE INDEX idx_kle_group_main_group ON kle_group (main_group_number);
CREATE INDEX idx_kle_subject_group ON kle_subject (group_number);