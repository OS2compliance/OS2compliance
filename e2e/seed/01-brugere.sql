-- Faste e2e-brugere. Rollerne er dem en IdP ville sende som claims — RolePostProcessor
-- folder dem ud til de egentlige rettigheder ved login, præcis som ved SAML.
-- Opdaterer i stedet for at slette og indsætte: users(uuid) hænger i en række fremmednøgler,
-- og en genstart uden --nulstil ville ellers vælte på en opgave med en e2e-bruger som ansvarlig.
INSERT INTO users (uuid, user_id, name, email, active, roles) VALUES
  ('e2e00000-0000-0000-0000-000000000001', 'e2e-admin',  'E2E Administrator', 'e2e-admin@example.invalid',  1, 'ROLE_administrator,ROLE_forandre,ROLE_adgang'),
  ('e2e00000-0000-0000-0000-000000000002', 'e2e-super',  'E2E Superbruger',   'e2e-super@example.invalid',  1, 'ROLE_forandre,ROLE_adgang'),
  ('e2e00000-0000-0000-0000-000000000003', 'e2e-bruger', 'E2E Almindelig',    'e2e-bruger@example.invalid', 1, 'ROLE_adgang')
ON DUPLICATE KEY UPDATE
  name = VALUES(name), email = VALUES(email), active = VALUES(active), roles = VALUES(roles);
