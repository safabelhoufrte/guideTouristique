-- Compte de demonstration "demo-guide" (id fixe dans infra/keycloak/guide-touristique-realm.json)
-- lie au guide de demonstration 1 (Youssef Benali), pour que l'Espace Guide fonctionne
-- des la premiere installation. Rien ne change si le guide 1 a deja un compte
-- ou si ce compte est deja lie a un autre guide.
UPDATE guide
SET owner_subject = '22222222-2222-4222-8222-222222222222'
WHERE id = 1
  AND owner_subject IS NULL
  AND NOT EXISTS (
    SELECT 1 FROM guide WHERE owner_subject = '22222222-2222-4222-8222-222222222222'
  );
