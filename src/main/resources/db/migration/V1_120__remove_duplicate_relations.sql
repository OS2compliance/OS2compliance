-- Fjern dublerede relations-raekker.
--
-- `relations` har ingen unique constraint, og relationer blev gemt uden at tjekke om parret allerede
-- fandtes. Flere kaldsteder tilfoejer relationer de ikke kan vide er der i forvejen, saa samme par
-- kunne ende som to raekker, og entiteten blev derefter listet to gange paa relationslisterne.
--
-- Tre ting goer sletningen mindre trivielt end den ser ud, og de styrer hele udformningen nedenfor.
--
-- 1) Id'er er IKKE globalt unikke paa tvaers af Relatable-typer. `Relatable` bruger en delt
--    TABLE-generator, men suppliers, contacts, threat_assessment_responses, dbs_asset, dbs_oversight
--    og dpia er oprettet med deres egen auto_increment-PK. V1_52__fix_dpia_ids.sql er netop en
--    oprydning efter at dpia havde faaet id'er der kolliderede med det delte sekvenssegment — og kun
--    dpia blev rettet. `RelationCleanupService.findAllDuplicateIds` findes for at opdage netop den
--    slags kollisioner. Derfor normaliseres parret paa (id, type)-tupler og ikke paa id alene: ellers
--    ville en relation til Task 42 og en relation til Supplier 42 blive regnet som dubletter, og den
--    ene — en fuldstaendig legitim relation — ville blive slettet.
--
-- 2) `relation_properties` haenger paa en konkret relations.id med ON DELETE CASCADE
--    (V1_25__update_cascade_delete.sql). En relations-raekke er derfor ikke ren kobling: den kan
--    baere `riskScale`, brugerens vaegtning af et aktivs konsekvens i en fortegnelses risikovurdering
--    (RegisterAssetAssessmentService). Sletter vi den raekke der baerer vaegtningen, forsvinder
--    brugerens indstilling tavst og fortegnelsens vurdering aendrer sig. Vi beholder derfor den raekke
--    der har properties, og lader grupper hvor FLERE raekker har properties vaere helt urorte — der er
--    ingen sikker maade at vaelge mellem to saet brugerindtastede vaerdier.
--
-- 3) `relation_a_name`/`relation_b_name` er denormaliserede kopier af entitetens navn, og de skrives
--    kun ved oprettelse — ingen kode opdaterer dem naar en entitet omdoebes. Er en dublet oprettet
--    efter en omdoebning, har den nyeste raekke det friske navn og den aeldste et forael­det. Navnet
--    vises i `related_entities` i R__view_gui_datatables.sql, saa naar properties ikke peger paa en
--    bestemt raekke, beholder vi den NYESTE.
--
-- WITH-blokken ligger inde i IN-subquerien, ikke foran DELETE. MariaDB (10.6 i drift) tillader kun
-- WITH foran en SELECT, saa `WITH ... DELETE` — som MySQL 8 accepterer — fejler med 1064. Formen her
-- har den sidegevinst at begge CTE'er materialiseres (`normalized` refereres to gange,
-- `duplicate_groups` har GROUP BY), saa vi ikke rammer 1093 ved at laese `relations` mens vi sletter
-- i den.

DELETE
FROM relations
WHERE id IN (WITH normalized AS (SELECT r.id,
                                        LEAST(CONCAT(LPAD(r.relation_a_id, 20, '0'), ':', r.relation_a_type),
                                              CONCAT(LPAD(r.relation_b_id, 20, '0'), ':',
                                                     r.relation_b_type))    AS low_key,
                                        GREATEST(CONCAT(LPAD(r.relation_a_id, 20, '0'), ':', r.relation_a_type),
                                                 CONCAT(LPAD(r.relation_b_id, 20, '0'), ':',
                                                        r.relation_b_type)) AS high_key,
                                        EXISTS (SELECT 1
                                                FROM relation_properties p
                                                WHERE p.relation_id = r.id) AS has_properties
                                 FROM relations r),
                  duplicate_groups AS (SELECT low_key,
                                              high_key,
                                              MIN(CASE WHEN has_properties = 1 THEN id END) AS keep_with_properties,
                                              MAX(id)                                       AS keep_newest
                                       FROM normalized
                                       GROUP BY low_key, high_key
                                       HAVING COUNT(*) > 1
                                          AND SUM(has_properties) <= 1)
             SELECT n.id
             FROM normalized n
                      JOIN duplicate_groups g
                           ON g.low_key = n.low_key AND g.high_key = n.high_key
             WHERE n.id <> COALESCE(g.keep_with_properties, g.keep_newest));
