CREATE TABLE register_kle_main_group
(
    register_id BIGINT NOT NULL,
    kle_main_group_number VARCHAR(50) NOT NULL,
    PRIMARY KEY (register_id, kle_main_group_number),
    CONSTRAINT fk_register_kle_main_group_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_kle_main_group_kle_main_group_number FOREIGN KEY (kle_main_group_number) REFERENCES kle_main_group (main_group_number) ON DELETE CASCADE
);

CREATE TABLE register_kle_group
(
    register_id BIGINT NOT NULL,
    kle_group_number VARCHAR(50) NOT NULL,
    PRIMARY KEY (register_id, kle_group_number),
    CONSTRAINT fk_register_kle_group_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_kle_group_kle_group_number FOREIGN KEY (kle_group_number) REFERENCES kle_group (group_number) ON DELETE CASCADE
)