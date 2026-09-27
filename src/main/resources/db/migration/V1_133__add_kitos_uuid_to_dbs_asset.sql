-- Kitos-uuid'et findes kun i audit-payloadens systems[], og mapningen til et IT-aktiv sker
-- derfor kun for systemer i det aktuelle hentevindue. Persisteret her kan koblingen
-- genforsoeges ved hver koersel, saa et fejlet match (fx fordi Kitos endnu ikke var slaaet til)
-- ikke kraever en manuel nulstilling af vandmaerket dbs_platform_last_sync.
ALTER TABLE dbs_asset ADD COLUMN kitos_uuid VARCHAR(36) NULL;
