-- Relatable-subklasser traekker id fra det delte segment i hibernate_sequences, saa auto_increment
-- fyrer aldrig. Attributten er efterladt DDL, og det er den vej id'erne kom skaevt ind i det delte
-- id-rum. V1_55__dpia_id_fix.sql fjernede den fra dpia og dbs_asset; contacts og suppliers blev
-- glemt. FK-tjekket er slaaet fra fordi MySQL ellers afviser aendringen med fejl 1833 saa laenge
-- assets_suppliers og supplier_tag peger paa suppliers.id. Typen er uaendret, saa data roeres ikke.

SET FOREIGN_KEY_CHECKS = 0;

ALTER TABLE contacts MODIFY id BIGINT NOT NULL;
ALTER TABLE suppliers MODIFY id BIGINT NOT NULL;

SET FOREIGN_KEY_CHECKS = 1;
