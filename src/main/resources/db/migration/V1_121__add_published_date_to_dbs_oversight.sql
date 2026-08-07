-- Platformens publishedDate gemmes separat: created paa raekker adopteret fra den gamle
-- integration er usammenlignelig med den nye APIs publishedDate. Genudgivelses-detektionen
-- keyes paa denne kolonne; NULL betyder at platform-syncen ikke har set raekken endnu.
ALTER TABLE dbs_oversight ADD COLUMN published_date datetime NULL;
