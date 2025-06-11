-- Update dpia_screening table to reference the new DPIA IDs
UPDATE dpia_screening
SET dpia_id = (
    SELECT d.id FROM dpia d WHERE d.temp_old_id = dpia_screening.dpia_id
);
