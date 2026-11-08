# Circuit : intégration de main2 et sécurité

Cette branche conserve l'accueil, les pages publiques Guide/Avis et les photos
de `main2`. Elle ajoute le backend Circuit et protège ses écritures avec la
même configuration JWT que la référence `auth-integration`. Le frontend public
ne contient pas les sessions Keycloak ni les espaces privés : ceux-ci restent
sur `auth-integration`. Les autres backends ne sont pas importés par cette PR.

## Accès API

| Opération | Autorisation |
|---|---|
| GET `/api/circuits` et `/api/circuits/{id}` | Publique |
| POST `/api/circuits` | ADMIN |
| PUT et DELETE `/api/circuits/{id}` | ADMIN |
| Swagger et `/actuator/health` | Publics |

Un appel privé sans token reçoit 401 ; un token valide sans ADMIN reçoit 403.
Le backend vérifie signature, expiration, émetteur, audience et sujet.
DELETE archive le circuit. Les appels Guide restent des lectures publiques,
avec délais et gestion des erreurs ; un guide indisponible n'est pas confondu
avec un guide inexistant. Le CORS est défini dans `SecurityConfig` uniquement.

## Base et Keycloak

- Docker : `jdbc:postgresql://postgres:5432/tourisme_db`.
- IntelliJ : `jdbc:postgresql://localhost:15433/tourisme_db` par défaut.
- Compte `circuit`, schéma `circuit`, historique `flyway_circuit_history`.
- Fournir le mot de passe du compte Circuit, jamais celui de l'administrateur.
- Les migrations V1 et V2 existantes restent inchangées.

Pour un volume existant, suivre [la transition de la base partagée](shared-database.md)
avant de redémarrer ; ne pas supprimer les volumes.

Les propriétés Spring disposent des valeurs locales suivantes :

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8180/realms/guide-touristique
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://localhost:8180/realms/guide-touristique/protocol/openid-connect/certs
spring.security.oauth2.resourceserver.jwt.audiences=guide-touristique-api
```

Dans Docker, Compose conserve l'émetteur visible du navigateur, mais charge les
clés via `http://keycloak:8080/realms/guide-touristique/protocol/openid-connect/certs`.
`AUTH_ISSUER_URI` et `AUTH_JWK_SET_URI` permettent de configurer ces deux adresses.
Pour IntelliJ, garder les clés sur `localhost:8180` ; ne pas copier la valeur
Docker `keycloak:8080` dans le processus lancé sur le PC.

Le Compose de cette branche ne crée pas Keycloak. Pour une vérification commune,
préparer et lancer l'infrastructure selon le
[README de auth-integration](https://github.com/safabelhoufrte/guideTouristique/blob/auth-integration/README.md),
notamment `python scripts/setup-local-auth.py` puis
`docker compose up -d postgres keycloak-db keycloak` depuis cette référence.
Les deux Compose utilisent le projet `guide-touristique-main2` : ils partagent
réseau et volumes, ce ne sont pas deux installations isolées. Conserver le même
environnement de base et ne pas démarrer deux versions du même service.
Les comptes locaux sont dans `.local/demo-accounts.txt` de cette installation.

Après préparation des mots de passe, Circuit et le frontend public peuvent être
lancés depuis cette branche :

```powershell
docker compose up --build -d circuit-service frontend
```

L'existence d'un vrai guide doit être fournie par l'API Guide intégrée. Pour un
essai explicite avec le simulateur uniquement :

```powershell
docker compose -f docker-compose.yml -f compose.demo.yaml up --build -d guide-service circuit-service frontend
```

Cette variante simule Guide, pas l'authentification ; Keycloak reste nécessaire
pour les écritures avec un vrai access token ADMIN. Le catalogue vide et les
lectures publiques fonctionnent sans connexion Keycloak ; la récupération des
clés est déclenchée lorsqu'un Bearer token doit être vérifié.

## Scripts et vérifications

Les deux scripts qui créent des circuits exigent désormais `DEMO_ADMIN_TOKEN` :

```powershell
$env:DEMO_ADMIN_TOKEN = '<access token ADMIN de votre session locale>'
python circuit-service/scripts/seed-demo.py --url http://localhost:8082
python circuit-service/scripts/demo-client.py --url http://localhost:8082
Remove-Item Env:DEMO_ADMIN_TOKEN
```

Le script `demo-client.py` suppose le guide de test 1 ; `seed-demo.py` utilise
les guides 1 et 2. Ne pas publier les tokens ni les mots de passe.

Dans `circuit-service`, exécuter `mvn verify` avec Docker Desktop démarré.
Les tests utilisent PostgreSQL 16, un simulateur Guide et des JWT RSA signés
vérifiés via un serveur JWKS local. Ils couvrent les écritures ADMIN, les refus
401/403 sur POST/PUT/DELETE, les jetons invalides, les lectures publiques,
le CORS, les contrats, les délais, Flyway et la persistance après redémarrage.
Ils ne remplacent pas le parcours final avec le vrai Guide et Keycloak.

Dans `frontend` : `npm ci`, `npm test -- --watch=false`, puis `npm run build`.
GitHub Actions vérifie Circuit et le frontend partagé.
