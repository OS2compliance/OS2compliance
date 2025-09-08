CREATE TABLE chart_configuration (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    section VARCHAR(255) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(255) NOT NULL,
    aggregation VARCHAR(255) NULL,
    selectable_axis VARCHAR(255) NOT NULL,
    allowed_x_field_choices VARCHAR(255) NULL,
    allowed_y_field_choices VARCHAR(255) NULL,
    selectable_period VARCHAR(255) NOT NULL,
    group_time_by_field VARCHAR(255) NULL,
    default_start_time VARCHAR(255) NOT NULL,
    default_end_time VARCHAR(255) NOT NULL,
    owner_only BOOLEAN DEFAULT FALSE
);

CREATE INDEX IF NOT EXISTS idx_chart_config_entity_name ON chart_configuration (entity_name);