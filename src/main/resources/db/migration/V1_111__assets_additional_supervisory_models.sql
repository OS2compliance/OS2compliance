CREATE TABLE assets_additional_supervisory_models (
    asset_id BIGINT NOT NULL,
    choice_value_id BIGINT NOT NULL,
    PRIMARY KEY (asset_id, choice_value_id),
    CONSTRAINT fk_add_sup_model_asset FOREIGN KEY (asset_id) REFERENCES assets(id),
    CONSTRAINT fk_add_sup_model_choice FOREIGN KEY (choice_value_id) REFERENCES choice_values(id)
);