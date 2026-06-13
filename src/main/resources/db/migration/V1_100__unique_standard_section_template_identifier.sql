-- Backstop mod dublerede StandardSections paa samme template-sektion.
-- StandardSection.templateSection er en @OneToOne paa template_section_identifier;
-- to raekker med samme vaerdi faar /standards til at crashe med
-- "More than one row with the given identifier was found".
-- Forudsaetter at eksisterende dubletter er ryddet foerst.
ALTER TABLE standard_sections
    ADD CONSTRAINT uq_standard_sections_template_section
    UNIQUE (template_section_identifier);
