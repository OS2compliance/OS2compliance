CREATE TABLE assets_product_links (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    asset_id BIGINT NOT NULL,
    CONSTRAINT fk_assets_product_links_asset_id FOREIGN KEY (asset_id) REFERENCES assets(id) ON DELETE CASCADE
);

INSERT INTO assets_product_links (url, asset_id)
SELECT product_link, id FROM assets
WHERE product_link IS NOT NULL AND TRIM(product_link) != '';

ALTER TABLE assets DROP COLUMN product_link;
