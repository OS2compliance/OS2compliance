-- Fjern dublerede relations-raekker.
--
-- `relations` har ingen unique constraint, og addRelation gemte blindt uden at tjekke om relationen
-- fandtes. Flere kaldsteder tilfoejer relationer de ikke kan vide allerede er der, saa samme par kunne
-- ende som to raekker. Da findAllRelatedTo mapper hver raekke til sin entitet uden dedup, blev den
-- samme opgave eller det samme aktiv listet flere gange paa relationslisterne.
--
-- En relations-raekke er ren kobling uden egen betydning, saa dubletter kan fjernes uden tab. Vi
-- beholder den aeldste raekke (laveste id) pr. par. Navnekolonnerne er backfyldt for alle raekker i
-- V1_109, saa den aeldste raekke er lige saa komplet som de nyere.
--
-- Parret normaliseres med LEAST/GREATEST, saa et par ogsaa fanges naar de to raekker har byttet om paa
-- A og B. Gruppering paa id alene er tilstraekkelig: alle Relatable-subklasser deler den samme
-- id-generator, saa et id peger paa praecis en entitet og dermed en entydig type.

DELETE r
FROM relations r
    JOIN (SELECT LEAST(relation_a_id, relation_b_id)    AS low_id,
                 GREATEST(relation_a_id, relation_b_id) AS high_id,
                 MIN(id)                               AS keep_id
          FROM relations
          GROUP BY LEAST(relation_a_id, relation_b_id), GREATEST(relation_a_id, relation_b_id)
          HAVING COUNT(*) > 1) d
        ON d.low_id = LEAST(r.relation_a_id, r.relation_b_id)
            AND d.high_id = GREATEST(r.relation_a_id, r.relation_b_id)
WHERE r.id <> d.keep_id;
