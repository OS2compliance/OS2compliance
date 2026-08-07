-- Kobler en oversight til de DBS-systemer auditens systems[] daekker, saa opgaver og
-- auditlinks kun rammer de rigtige systemer. Tom maengde = aeldre raekke uden systemdata;
-- opgavejobbet falder da tilbage til leverandoer-bred adfaerd.
CREATE TABLE dbs_oversight_assets (
    dbs_oversight_id BIGINT NOT NULL,
    dbs_asset_id BIGINT NOT NULL,
    PRIMARY KEY (dbs_oversight_id, dbs_asset_id),
    -- CASCADE: dbs_oversight/dbs_asset slettes selv ved kaskade fra dbs_supplier - uden den
    -- ville join-raekkerne blokere de eksisterende ON DELETE CASCADE-kaeder
    CONSTRAINT fk_dbs_oversight_assets_oversight FOREIGN KEY (dbs_oversight_id) REFERENCES dbs_oversight(id) ON DELETE CASCADE,
    CONSTRAINT fk_dbs_oversight_assets_asset FOREIGN KEY (dbs_asset_id) REFERENCES dbs_asset(id) ON DELETE CASCADE
);
