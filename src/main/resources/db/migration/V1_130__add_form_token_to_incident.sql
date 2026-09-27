-- Unik nøgle fra oprettelsesformularen: to samtidige indsendelser kan kun blive til én hændelse.
ALTER TABLE incidents ADD COLUMN form_token VARCHAR(36) NULL;
CREATE UNIQUE INDEX ux_incidents_form_token ON incidents (form_token);
