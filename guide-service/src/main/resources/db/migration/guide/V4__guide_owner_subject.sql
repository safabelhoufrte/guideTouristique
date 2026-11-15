-- Lien entre un compte Keycloak (claim "sub", UUID) et un profil guide.
-- NULL : guide sans compte. Unique : un compte est lie a un seul guide au plus.
ALTER TABLE guide ADD COLUMN owner_subject VARCHAR(36);
CREATE UNIQUE INDEX ux_guide_owner_subject ON guide (owner_subject);
