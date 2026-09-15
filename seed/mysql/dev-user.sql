-- Seed the admin user matching the Keycloak test user (NameID = username = UUID).
-- Applied by the db-seed compose service once the os2compliance Flyway migrations
-- have created the users table (NOT via mysql initdb — the table doesn't exist then).
-- Idempotent: safe to re-run. Reset with docker compose down -v.
INSERT INTO users (uuid, active, name, email, user_id)
VALUES ('00000000-0000-4000-8000-000000000001', true, 'Admin User', 'admin@os2compliance.local', 'admin')
ON DUPLICATE KEY UPDATE active = true, name = VALUES(name), email = VALUES(email), user_id = VALUES(user_id);
