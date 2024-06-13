
CREATE TABLE relation_properties
(
    id          BIGINT AUTO_INCREMENT NOT NULL,
    prop_key    VARCHAR(255)          NOT NULL,
    prop_value  VARCHAR(255)          NOT NULL,
    relation_id BIGINT                NOT NULL,
    CONSTRAINT pk_relation_properties PRIMARY KEY (id)
);

ALTER TABLE relation_properties
    ADD CONSTRAINT FK_RELATION_PROPERTIES_ON_RELATION FOREIGN KEY (relation_id) REFERENCES relations (id);

create index index_rel_properties_key_value
    on relation_properties (prop_key, prop_value);