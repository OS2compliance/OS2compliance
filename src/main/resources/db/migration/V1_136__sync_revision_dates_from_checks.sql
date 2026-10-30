-- Revision dates were only copied from their revision check when the revision dialog was opened
UPDATE threat_assessments ta
    JOIN properties p ON p.prop_key = 'linked_threat' AND CAST(p.prop_value AS UNSIGNED) = ta.id
    JOIN tasks t ON t.id = p.entity_id AND t.deleted = false
SET ta.next_revision = t.next_deadline
WHERE ta.next_revision IS NULL OR ta.next_revision <> t.next_deadline;

UPDATE dpia d
    JOIN properties p ON p.prop_key = 'linked_dpia' AND CAST(p.prop_value AS UNSIGNED) = d.id
    JOIN tasks t ON t.id = p.entity_id AND t.deleted = false
SET d.next_revision = t.next_deadline
WHERE d.next_revision IS NULL OR d.next_revision <> t.next_deadline;
