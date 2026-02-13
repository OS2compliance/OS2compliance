CREATE TABLE choice_measure_category (
                                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
                                         sort_order INT NOT NULL DEFAULT 0,
                                         deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                         INDEX idx_sort_order (sort_order, deleted)
) COLLATE = utf8mb4_danish_ci;

ALTER TABLE choices_measures
    ADD COLUMN sort_order INT NOT NULL DEFAULT 0,
ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN category_id BIGINT NULL;

ALTER TABLE assets
    ADD COLUMN asset_measure_status VARCHAR(50) NULL;

INSERT INTO choice_measure_category (name, sort_order)
SELECT
    category,
    ROW_NUMBER() OVER (ORDER BY category) as sort_order
FROM (
         SELECT DISTINCT category
         FROM choices_measures
     ) distinct_categories;

UPDATE choices_measures cm
    INNER JOIN choice_measure_category cmc ON cm.category = cmc.name
    SET cm.category_id = cmc.id;

UPDATE choices_measures
SET sort_order = id;

ALTER TABLE choices_measures
    ADD CONSTRAINT fk_choices_measures_category
        FOREIGN KEY (category_id) REFERENCES choice_measure_category(id);

ALTER TABLE choices_measures
    MODIFY COLUMN category_id BIGINT NOT NULL;

CREATE INDEX idx_choices_measures_sorting
    ON choices_measures(category_id, sort_order, deleted);

ALTER TABLE choices_measures
DROP COLUMN category;