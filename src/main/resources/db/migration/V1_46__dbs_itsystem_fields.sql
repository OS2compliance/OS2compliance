

ALTER TABLE dbs_asset ADD COLUMN status VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_danish_ci NULL;
ALTER TABLE dbs_asset ADD COLUMN next_revision DATE NULL;