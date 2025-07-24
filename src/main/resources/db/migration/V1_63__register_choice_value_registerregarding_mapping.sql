CREATE TABLE register_choice_value_registerregarding_mapping
(
    register_id BIGINT(20)  NOT NULL,
    choice_value_id    BIGINT(20)  NOT NULL,
    PRIMARY KEY (register_id, choice_value_id),
    CONSTRAINT fk_register_registerregarding_mapping_register_id FOREIGN KEY (register_id) REFERENCES registers (id) ON DELETE CASCADE,
    CONSTRAINT fk_register_registerregarding_mapping_choice_value_id FOREIGN KEY (choice_value_id) REFERENCES choice_values (id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;