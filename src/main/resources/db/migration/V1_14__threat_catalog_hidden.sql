
ALTER TABLE threat_catalogs ADD COLUMN hidden BIT DEFAULT b'0';
ALTER TABLE threat_catalogs ADD COLUMN deleted BIT DEFAULT b'0';
