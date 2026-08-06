-- Kobler en oversight til de DBS-systemer auditens systems[] daekker. Uden koblingen fanner
-- opgavejobbet ud til ALLE leverandoerens aktiver, saa auditlinks og opgaver lander paa systemer
-- auditen ikke daekker. En tom maengde betyder en aeldre raekke uden systemdata; opgavejobbet
-- falder da tilbage til den leverandoer-brede adfaerd.
CREATE TABLE dbs_oversight_assets (
    dbs_oversight_id BIGINT NOT NULL,
    dbs_asset_id BIGINT NOT NULL,
    PRIMARY KEY (dbs_oversight_id, dbs_asset_id),
    CONSTRAINT fk_dbs_oversight_assets_oversight FOREIGN KEY (dbs_oversight_id) REFERENCES dbs_oversight(id),
    CONSTRAINT fk_dbs_oversight_assets_asset FOREIGN KEY (dbs_asset_id) REFERENCES dbs_asset(id)
);
