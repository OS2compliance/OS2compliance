-- 'deleted' is mapped as a primitive boolean and queried with 'deleted = false', which in MySQL does
-- not match NULL. Align the column with the mapping so a soft deleted question is filtered the same
-- way in SQL and in Java.
UPDATE dpia_template_question SET deleted = FALSE WHERE deleted IS NULL;
ALTER TABLE dpia_template_question MODIFY deleted BOOLEAN NOT NULL DEFAULT FALSE;
