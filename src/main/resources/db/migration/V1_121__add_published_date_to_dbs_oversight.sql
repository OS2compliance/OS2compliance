-- Platformens publishedDate gemmes separat: created paa raekker adopteret fra den gamle
-- integration er usammenlignelig med den nye APIs publishedDate. Genudgivelses-detektionen
-- keyes paa denne kolonne; NULL betyder at platform-syncen ikke har set raekken endnu.
-- datetime(6): API'et kan levere braekdele af sekunder; med sekund-praecision ville en afkortet
-- vaerdi ligne et fremadrettet hop ved naeste resync (falsk genudgivelse).
ALTER TABLE dbs_oversight ADD COLUMN published_date datetime(6) NULL;
