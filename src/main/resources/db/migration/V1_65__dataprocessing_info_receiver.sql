CREATE TABLE data_processing_info_receiver
(
    id                                       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    receiver_location                        VARCHAR(255) NOT NULL,
    choice_value_id                          BIGINT       NOT NULL,
    data_processing_categories_registered_id BIGINT       NOT NULL,
    CONSTRAINT fk_info_receiver_choice_value_id FOREIGN KEY (choice_value_id) REFERENCES choice_values (id) ON DELETE CASCADE,
    CONSTRAINT fk_info_receiver_categories_registered_id FOREIGN KEY (data_processing_categories_registered_id) REFERENCES data_processing_categories_registered (id) ON DELETE CASCADE
) collate = utf8mb4_danish_ci;;

ALTER TABLE data_processing_categories_registered
    RENAME COLUMN information_receivers TO information_receivers_old;

ALTER TABLE data_processing_categories_registered
    ADD COLUMN receiver_comment VARCHAR(255);