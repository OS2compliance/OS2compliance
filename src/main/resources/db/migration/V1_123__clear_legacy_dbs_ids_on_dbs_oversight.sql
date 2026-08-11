-- Gamle raekkers dbs_id er Document-id'er fra den nedlagte integration, i samme talrum som
-- platformens audit-id'er - ved kollision kaprede en audit raekken (navn/link overskrevet,
-- forkert leverandoer beholdt). Raekker uden audit_link er aldrig roert af platform-syncen
-- (den saetter altid linket); deres dbs_id nulles, saa de ikke kan kapres. Cutover-adoption
-- via navn+leverandoer virker uaendret.
ALTER TABLE dbs_oversight MODIFY COLUMN dbs_id BIGINT NULL;
UPDATE dbs_oversight SET dbs_id = NULL WHERE audit_link IS NULL;
