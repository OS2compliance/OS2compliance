ALTER TABLE choice_lists
ADD COLUMN customizable BIT(1) NOT NULL DEFAULT b'0' COLLATE utf8mb4_danish_ci;

UPDATE choice_lists
SET customizable = b'0';