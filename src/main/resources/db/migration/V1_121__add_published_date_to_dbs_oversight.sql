-- Platformens publishedDate gemmes separat fra created: created på rækker adopteret fra den
-- gamle DBS-integration stammer fra det gamle systems dokumentdato og er ikke sammenlignelig
-- med den nye APIs publishedDate. Genudgivelses-detektionen keyes på denne kolonne, så vi kun
-- sammenligner platform-datoer med platform-datoer. NULL betyder at rækken endnu ikke er set
-- af platform-syncen efter denne ændring.
ALTER TABLE dbs_oversight ADD COLUMN published_date datetime NULL;
