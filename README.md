# Guide Touristique — intégrer l’authentification ensemble

Bienvenue Safa, Nada, Mariem et Hajar ! Cette branche `auth-integration` rassemble une version locale de l’application avec connexion, comptes et permissions. Elle sert de référence pour ajouter l’authentification à vos services sans perdre le travail que chacune a déjà réalisé.

**Vous n’avez pas besoin de réécrire votre service.** Gardez vos fonctionnalités et votre organisation, puis adaptez les éléments de sécurité expliqués ci-dessous. Les versions Guide, Réservation et Avis présentes ici sont des propositions d’intégration à relire avec leurs responsables, pas des remplacements imposés.

| Branche | Ce qu’on y trouve |
|---|---|
| `main2` | Le frontend public partagé, les photos et la configuration commune de première installation PostgreSQL |
| `circuit-service` | Le service Circuit de Nada et ses pages publiques |
| `auth-integration` | La connexion Keycloak, les espaces privés et une intégration de référence des quatre API |

Ne fusionnez pas toute cette branche dans `main2` pour récupérer seulement la connexion. Avancez service par service, avec une petite Pull Request relue par sa responsable.

## Par où commencer ?

Pour Nada : [démarrage et validation du parcours admin Circuit](docs/circuit-admin-validation.md),
avec un scénario navigateur et un contrôle reproductible contre les vrais services.

1. **Pour voir le résultat :** suivez [le démarrage local](#1-démarrer-la-version-avec-connexion).
2. **Pour comprendre :** lisez [le parcours d’une connexion](#2-comment-la-connexion-fonctionne).
3. **Pour coder :** appliquez [les étapes communes](#3-ce-que-chaque-service-spring-boot-doit-ajouter), puis [la partie de votre service](#4-la-partie-de-chacune).
4. **Avant de partager :** utilisez [les vérifications](#7-vérifier-que-tout-fonctionne), puis [le parcours Git](#8-intégrer-sans-écraser-le-travail-des-autres).

## 1. Démarrer la version avec connexion

### Ce qu’il faut sur votre PC

- Git, Docker Desktop démarré et Python 3.
- Pour développer hors Docker : Java 21 et Maven pour les services, Node.js 24 pour Angular.
- Les ports locaux 4200 et 8180 disponibles. Les ports des API sont indiqués plus bas.

### Première installation, dans un dossier de test

Si vous avez des modifications non enregistrées, gardez votre dossier actuel. Le plus simple pour découvrir cette branche est un nouveau clone :

```powershell
git clone --branch auth-integration https://github.com/safabelhoufrte/guideTouristique.git guide-touristique-auth
cd guide-touristique-auth
Copy-Item .env.example .env
```

Ouvrez `.env` et remplacez les valeurs `replace-...` de `DB_PASSWORD`, `GUIDE_DB_PASSWORD`, `CIRCUIT_DB_PASSWORD`, `RESERVATION_DB_PASSWORD` et `AVIS_DB_PASSWORD` par vos mots de passe locaux.

Puis, depuis la racine du projet :

```powershell
python scripts/setup-local-auth.py
docker compose up --build -d
docker compose ps
```

Le script génère les trois mots de passe Keycloak manquants dans `.env` et écrit les accès dans `.local/demo-accounts.txt`. **Il ne crée pas les mots de passe des bases métier et ne les remplace pas.** N’envoyez ni `.env` ni `.local/` sur GitHub.

Attention si l’application tourne déjà sur votre PC : les clones utilisent le même nom de projet Compose et les mêmes ports. Un nouveau dossier ne suffit pas à isoler les conteneurs. Arrêtez votre pile de développement précédente avec `docker compose stop` depuis son dossier, ou préparez une configuration réellement séparée avant de lancer une seconde pile. Gardez ses volumes.

| Pour accéder à… | Adresse par défaut |
|---|---|
| La page d’accueil | http://localhost:4200/ |
| La connexion | http://localhost:4200/connexion |
| L’espace administrateur | http://localhost:4200/administration |
| L’espace guide | http://localhost:4200/espace-guide |
| L’espace touriste | http://localhost:4200/espace-touriste |
| La console de gestion Keycloak | http://localhost:8180/admin |
| Les API Guide / Circuit / Réservation / Avis | Ports 8081 / 8082 / 8083 / 8084 |

`CIRCUIT_HOST_PORT`, `RESERVATION_HOST_PORT` et `FRONTEND_HOST_PORT` permettent de changer les ports exposés sur le PC. Les ports internes Docker restent les mêmes. Les exemples de ce README utilisent les ports par défaut.

### Avec quels comptes essayer ?

| Compte | Utilisation |
|---|---|
| `demo-touriste` | Réserver et retrouver ses réservations / avis |
| `demo-guide` | Voir son profil et ses circuits / réservations associés |
| `demo-admin` | Gérer les guides, circuits, réservations et avis dans l’application |
| `demo-autre` | Second touriste, utile pour vérifier l’isolation entre utilisateurs |
| `local-admin` | Administrer Keycloak, dans sa console uniquement |

Les mots de passe sont dans votre propre `.local/demo-accounts.txt` : il n’y a pas de mot de passe partagé dans ce README. `local-admin` et `demo-admin` n’ont pas le même rôle ni le même usage.

Sur une installation neuve, l’import Keycloak crée les comptes de démonstration, mais **ne remplit pas les tables métier**. Avec `demo-admin`, créez d’abord un profil Guide, puis un Circuit associé. Pour que `demo-guide` voie ce profil, associez son identifiant Keycloak comme expliqué dans la partie Safa.

### Si vous avez déjà des données ou un realm Keycloak

**Ne supprimez pas vos volumes et n’utilisez pas `docker compose down -v`.**

- `db/init.sql` s’exécute seulement lors de la création d’une base PostgreSQL vide. Sur une base existante, prévoyez une sauvegarde et une migration des comptes/permissions avec l’équipe.
- Flyway applique les migrations métier nouvelles. Comparez-les à vos tables avant de les reprendre : un script `CREATE TABLE` déjà utilisé ne doit pas être recopié comme une nouvelle migration.
- Le fichier de realm est importé à la première création. Modifier ce JSON ne met pas à jour un realm déjà présent.
- Le script de préparation écrit les accès locaux ; il ne change pas les mots de passe des comptes déjà stockés dans Keycloak. Si vous avez changé ces mots de passe, utilisez ceux de votre installation.
- Pour appliquer le thème à un realm existant, attendez que Keycloak soit prêt, puis lancez `python scripts/apply-auth-theme.py`. Ce script applique le thème, les langues et le lien de retour, en conservant comptes, rôles et paramètres SMTP.

## 2. Comment la connexion fonctionne

Prenons un exemple : un touriste veut consulter ses réservations.

1. Il clique sur **Connexion**. Angular ouvre le parcours Keycloak ; l’identifiant et le mot de passe sont saisis chez Keycloak.
2. Après connexion, Angular reçoit un **access token JWT**, un jeton signé qui décrit le compte et ses rôles. Le parcours utilise le flux « authorization code » avec PKCE S256.
3. Angular ajoute ce jeton à ses appels vers nos API :

   ```http
   Authorization: Bearer <access_token>
   ```

4. L’API vérifie le jeton, puis vérifie que la personne a le droit de faire cette opération.
5. Pour « mes réservations », elle recherche uniquement les lignes appartenant au compte connecté.

Keycloak gère les identifiants et mots de passe. Chaque service garde ses données métier et leurs liens avec le compte. Aucun service n’a besoin d’enregistrer un mot de passe utilisateur.

### Les quatre informations à reconnaître dans le jeton

| Information | Signification dans notre projet |
|---|---|
| `iss` | L’émetteur : `http://localhost:8180/realms/guide-touristique` en local |
| `aud` | Le destinataire ; doit contenir `guide-touristique-api` |
| `sub` | L’identifiant stable du compte ; on s’en sert pour la propriété des données |
| `realm_access.roles` | Les rôles Keycloak : `USER`, `GUIDE`, `ADMIN` |

Le realm s’appelle `guide-touristique` et le client Angular `guide-touristique-web`. Le client est public, sans secret à mettre dans Angular. L’API attend un **access token**, pas l’ID token utilisé pour les informations de connexion.

**Un rôle et une propriété sont deux vérifications différentes.** Avoir le rôle USER permet de demander une réservation ; cela ne permet pas de lire celle d’un autre touriste. Le backend doit comparer le `sub` signé au propriétaire enregistré. L’e-mail ou un `userId` envoyé dans le JSON ne prouve pas l’identité.

## 3. Ce que chaque service Spring Boot doit ajouter

Faites cette adaptation sur une branche de travail de votre service. Les liens suivants montrent le code de référence ; adaptez les packages et les routes à votre projet.

Pour travailler seulement sur votre service depuis IntelliJ, vous pouvez lancer uniquement l’infrastructure de cette branche avec `docker compose up -d postgres keycloak-db keycloak`, après avoir préparé `.env`. Lancez ensuite votre service avec ses paramètres de base et JWT. Le frontend privé pourra être activé lors du test commun ; le frontend public de `main2` ne fournit pas encore les sessions authentifiées. N’exécutez pas en même temps la version Docker et la version IntelliJ du même service sur le même port.

### Étape A — ajouter les dépendances

Dans votre `pom.xml`, en gardant vos dépendances existantes :

```xml
<dependency>
  <groupId>org.springframework.boot</groupId>
  <artifactId>spring-boot-starter-oauth2-resource-server</artifactId>
</dependency>
<dependency>
  <groupId>org.springframework.security</groupId>
  <artifactId>spring-security-test</artifactId>
  <scope>test</scope>
</dependency>
```

Spring Boot gère les versions. Pas besoin de remplacer JPA par JDBC, ou l’inverse, pour intégrer l’authentification.

### Étape B — renseigner l’émetteur et les clés publiques

Dans `application.properties` :

```properties
spring.security.oauth2.resourceserver.jwt.issuer-uri=${AUTH_ISSUER_URI:http://localhost:8180/realms/guide-touristique}
spring.security.oauth2.resourceserver.jwt.jwk-set-uri=${AUTH_JWK_SET_URI:http://localhost:8180/realms/guide-touristique/protocol/openid-connect/certs}
spring.security.oauth2.resourceserver.jwt.audiences=guide-touristique-api
```

Dans l’environnement Docker de **chaque API** :

```yaml
AUTH_ISSUER_URI: http://localhost:8180/realms/guide-touristique
AUTH_JWK_SET_URI: http://keycloak:8080/realms/guide-touristique/protocol/openid-connect/certs
```

Pourquoi deux adresses ? `iss` doit correspondre exactement à l’adresse écrite dans le jeton reçu par le navigateur. Pour télécharger les clés, le conteneur appelle Keycloak par son nom Docker `keycloak:8080`. Une API lancée depuis IntelliJ utilise au contraire l’adresse `localhost:8180` pour ces clés.

Gardez aussi votre configuration de base. Sous Docker, chaque service utilise son utilisateur et son mot de passe, pas le compte PostgreSQL administrateur. Les quatre services utilisent désormais la base partagée `tourisme_db`, avec un schéma par service. Hors Docker, les valeurs par défaut ciblent `localhost:15433/tourisme_db` ; fournissez le `DB_PASSWORD` du service. Voir [la configuration et la transition des données](docs/shared-database.md) avant de relancer avec un volume existant. La base technique de Keycloak reste séparée.

### Étape C — adapter `SecurityConfig`

Prenez le fichier de votre service comme modèle :

- [Guide : SecurityConfig](guide-service/src/main/java/ma/tourisme/guide/config/SecurityConfig.java)
- [Circuit : SecurityConfig](circuit-service/src/main/java/ma/tourisme/circuit/config/SecurityConfig.java)
- [Réservation : SecurityConfig](reservation-service/src/main/java/ma/tourisme/reservation/config/SecurityConfig.java)
- [Avis : SecurityConfig](avis-service/src/main/java/ma/tourisme/avis/config/SecurityConfig.java)

Ces fichiers font trois choses importantes :

1. Ils vérifient la signature, l’expiration, l’émetteur, l’audience et la présence d’un `sub` non vide. Gardez les validateurs explicites : nos configurations créent leur propre `JwtDecoder`, donc copier seulement la propriété `audiences` ne suffit pas à reproduire la validation.
2. Ils transforment les rôles de `realm_access.roles` en autorités Spring : `ADMIN` devient `ROLE_ADMIN`. On peut alors utiliser `hasRole("ADMIN")`.
3. Ils définissent les routes publiques, les rôles autorisés et un refus par défaut pour les autres routes.

Placez les règles précises (`/me`, `/admin`, etc.) **avant** une règle générale comme `GET /api/guides/*`. Sinon, une route privée pourrait devenir publique par erreur.

Les API fonctionnent sans session serveur et reçoivent un Bearer token dans un en-tête. Le CSRF est désactivé pour ce fonctionnement précis ; ne reprenez pas ce choix sans réévaluation si vous utilisez plus tard une authentification par cookie. Le CORS local autorise `http://localhost:4200`, les méthodes attendues et les en-têtes `Authorization` / `Content-Type`. Préservez le traitement des requêtes OPTIONS du navigateur.

### Étape D — récupérer le compte connecté dans le contrôleur

Voici la forme utilisée dans Réservation :

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Reservation create(
    @Valid @RequestBody ReservationRequest request,
    @AuthenticationPrincipal Jwt jwt) {
  return service.creer(request, jwt);
}
```

Imports : `AuthenticationPrincipal` vient de `org.springframework.security.core.annotation` et `Jwt` de `org.springframework.security.oauth2.jwt`.

Dans la couche Service, `jwt.getSubject()` donne le propriétaire à enregistrer ou à vérifier. La règle d’accès doit fonctionner même si quelqu’un appelle directement l’API sans passer par Angular.

## 4. La partie de chacune

### Safa — Guide : relier un profil à son compte

Le rôle GUIDE dit « cette personne est guide ». Le champ métier `ownerSubject` dit **quel profil lui appartient**.

- Les lectures publiques affichent les profils actifs sans e-mail, téléphone ni identifiant Keycloak.
- ADMIN peut créer, modifier et archiver les guides, et associer leur compte.
- GUIDE peut lire/modifier son profil via `/api/guides/me` et consulter ses circuits via `/api/guides/me/circuits`.
- Le guide ne doit pas pouvoir changer lui-même son `ownerSubject` ni son tarif par cette route.

Pour faire l’association : ouvrez la console Keycloak, sélectionnez le realm `guide-touristique`, puis **Users**. Ouvrez le compte du guide, attribuez-lui le rôle de realm GUIDE dans **Role mapping**, puis copiez son ID. Avec un compte ADMIN dans l’application, placez cet ID dans `ownerSubject` du profil Guide. Un compte ne peut être associé qu’à un profil dans cette version.

Exemple : si Youssef se connecte, `/me` retrouve son profil par `sub`. Il ne reçoit pas un `guideId` arbitraire du navigateur.

À consulter : [contrôleur](guide-service/src/main/java/ma/tourisme/guide/controller/GuideController.java), [service](guide-service/src/main/java/ma/tourisme/guide/service/GuideService.java), [réponses publiques/privées](guide-service/src/main/java/ma/tourisme/guide/dto/GuideResponse.java), [migration](guide-service/src/main/resources/db/migration/guide/V1__create_guides.sql).

### Nada — Circuit : garder le catalogue public, protéger la gestion

- GET `/api/circuits` et GET `/api/circuits/{id}` restent publics.
- POST, PUT et DELETE demandent ADMIN. DELETE archive le circuit.
- Un circuit garde un seul `guideId`. Circuit vérifie l’existence du guide via Guide API.
- Dans le code actuel, Circuit ne refuse pas un guide uniquement parce que `disponible=false` ; c’est Réservation qui vérifie sa disponibilité au moment de réserver.

Reprenez ensemble le `pom.xml`, `SecurityConfig`, les propriétés JWT et les adaptations des tests. Le [script de démonstration](circuit-service/scripts/seed-demo.py) nécessite désormais un jeton ADMIN dans `DEMO_ADMIN_TOKEN`, conservé uniquement dans votre environnement local.

À consulter : [service](circuit-service/src/main/java/ma/tourisme/circuit/service/CircuitService.java), [test d’intégration](circuit-service/src/test/java/ma/tourisme/circuit/CircuitIntegrationTest.java).

### Mariem — Réservation : chaque touriste voit ses réservations

À la création, enregistrez `jwt.getSubject()` dans `user_subject`. Conservez vos données métier et ajoutez la propriété du compte sans la déduire du nom ou de l’e-mail.

| Route | Accès attendu |
|---|---|
| POST `/api/reservations` | USER ; propriétaire déduit du jeton |
| GET `/api/reservations/me` | USER ; uniquement ses réservations |
| GET `/api/reservations/guide` | GUIDE ; uniquement les réservations de son profil Guide |
| GET `/api/reservations` | ADMIN |
| GET `/api/reservations/{id}` | Propriétaire USER ou ADMIN |
| PATCH `/{id}/annuler` | Propriétaire USER ou ADMIN, selon date et statut |
| PATCH `/{id}/accepter` ou `/{id}/refuser` | ADMIN, pour une demande encore en attente et non passée |
| GET `/{id}/eligibilite` | Propriétaire USER ou ADMIN |

Les chemins abrégés du tableau commencent tous par `/api/reservations`.

Le navigateur envoie `circuitId`, `dateVisite`, `nbPersonnes`, `nom`, `email`. Le serveur déduit le guide et le prix depuis Circuit, vérifie la disponibilité auprès de Guide et calcule le total. Le client ne choisit ni propriétaire, ni prix total, ni statut.

Pour les réservations d’un guide, Réservation appelle Guide `/api/guides/me` avec **le même jeton**, puis utilise l’ID du profil retourné. Les anciennes réservations sans propriétaire restent visibles par ADMIN seulement. Le champ `version` protège les changements de statut concurrents.

À consulter : [contrôleur](reservation-service/src/main/java/ma/tourisme/reservation/controller/ReservationController.java), [service et propriété](reservation-service/src/main/java/ma/tourisme/reservation/service/ReservationService.java), [appel Guide](reservation-service/src/main/java/ma/tourisme/reservation/client/GuideClient.java), [migration V2](reservation-service/src/main/resources/db/migration/reservation/V2__reservation_ownership.sql).

### Hajar — Avis : vérifier la visite avant de publier

- GET `/api/avis` affiche seulement les avis publiés ; filtres possibles par circuit ou guide.
- POST `/api/avis` et GET `/api/avis/me` demandent USER.
- GET `/api/avis/admin` et PATCH `/api/avis/{id}/publication` demandent ADMIN.

Le formulaire envoie `{reservationId, noteGuide, noteCircuit, commentaire}`. Les notes sont des entiers de 1 à 5 et le commentaire contient au maximum 2 000 caractères. Le serveur déduit l’auteur depuis le jeton et les références du circuit/guide depuis Réservation.

Avant de créer un avis, appelez `/api/reservations/{id}/eligibilite` avec le jeton du touriste. Réservation vérifie que la réservation lui appartient, qu’elle est acceptée et que sa date est **strictement antérieure à aujourd’hui**, selon Africa/Casablanca. Un simple rôle USER ne suffit donc pas à publier sur n’importe quelle réservation.

Gardez une contrainte unique en base sur `reservation_id` : elle empêche deux avis pour la même visite, même avec deux requêtes simultanées. Le code de cette branche publie un nouvel avis immédiatement ; ADMIN peut ensuite le masquer. Une validation obligatoire avant publication serait un changement de règle à décider ensemble.

À consulter : [service](avis-service/src/main/java/ma/tourisme/avis/service/AvisService.java), [appel Réservation](avis-service/src/main/java/ma/tourisme/avis/client/ReservationClient.java), [migration](avis-service/src/main/resources/db/migration/avis/V1__create_avis.sql).

## 5. Quand un service appelle un autre service

Pour une lecture publique, le jeton n’est pas nécessaire. Pour agir au nom de la personne connectée sur une route privée, transmettez son access token :

```java
http.get()
    .uri("/api/reservations/{id}/eligibilite", reservationId)
    .header("Authorization", "Bearer " + jwt.getTokenValue())
    .retrieve();
```

Ce fragment montre l’ajout de l’en-tête ; reprenez aussi la lecture de la réponse, les délais et le traitement des erreurs du [client complet](avis-service/src/main/java/ma/tourisme/avis/client/ReservationClient.java).

| Appel actuel | Jeton transmis ? |
|---|---|
| Circuit → Guide `/{id}` | Non, lecture publique |
| Réservation → Circuit `/{id}` et Guide `/{id}` | Non, lectures publiques |
| Réservation → Guide `/me` | Oui, celui du guide connecté |
| Avis → Réservation `/{id}/eligibilite` | Oui, celui du touriste connecté |

Les API visées vérifient à nouveau le jeton et leurs permissions. N’utilisez pas un compte ADMIN partagé pour contourner ces contrôles. Les appels utilisent HTTP/JSON, pas l’accès direct à la base d’un autre service. Les clients actuels limitent la connexion à 2 secondes et l’attente de réponse à 3 secondes.

## 6. Ce qui est déjà fait côté Angular et Keycloak

- [AuthService](frontend/src/app/auth/auth.service.ts) initialise Keycloak, récupère les rôles, renouvelle le jeton et gère connexion/déconnexion.
- [L’intercepteur](frontend/src/app/auth/auth.interceptor.ts) ajoute le jeton aux URLs relatives `/api/…`, sans l’envoyer aux sites tiers ni aux images. Gardez ces URLs dans les services Angular.
- [Les guards](frontend/src/app/auth/auth.guard.ts) dirigent vers la connexion ou le bon espace. Ils améliorent la navigation ; les permissions restent contrôlées côté API.
- [La configuration publique](frontend/public/auth-config.json) contient l’URL Keycloak, le realm et le client, sans secret.
- [Le realm de référence](infra/keycloak/guide-touristique-realm.json) configure les rôles, l’audience API, le client public et les comptes de démonstration.
- [Le thème](infra/keycloak/themes/guide-touristique/login) donne aux formulaires Keycloak le style de l’application.

L’inscription publique attribue uniquement USER. GUIDE et ADMIN se gèrent dans Keycloak. Les comptes de démonstration sont déjà marqués comme vérifiés ; l’envoi de véritables e-mails nécessite encore un serveur SMTP et un test de livraison. N’activez l’obligation de vérifier l’e-mail qu’une fois cet envoi fonctionnel.

Si vous changez l’adresse ou le port du frontend, adaptez ensemble la configuration Angular, les redirect URIs / web origins du client Keycloak et le CORS des API. Si vous changez l’adresse de Keycloak, adaptez aussi `KC_HOSTNAME`, l’émetteur JWT et l’adresse des clés.

## 7. Vérifier que tout fonctionne

### Tests automatiques

Dans le dossier du service que vous adaptez, par exemple :

```powershell
cd reservation-service
mvn verify
```

Docker Desktop doit fonctionner pour les tests PostgreSQL avec Testcontainers. Les tests de référence se trouvent dans les quatre dossiers `src/test/java` : Circuit vérifie notamment les JWT falsifiés, expirés, sans sujet ou avec une mauvaise audience ; les autres services couvrent leurs permissions et règles métier. Complétez ces tests si vos endpoints diffèrent.

Dans `frontend` :

```powershell
npm ci
npm test -- --watch=false
npm run build
```

Pour lancer Angular hors Docker : `npm start`. Le [proxy de développement](frontend/proxy.conf.cjs) utilise les ports équipe. Par exemple, si Circuit écoute sur 18092 sur votre PC : `$env:CIRCUIT_API_URL = 'http://localhost:18092'` avant `npm start`.

### Petit parcours à faire ensemble

1. Sans connexion, consulter les circuits et les guides ; les avis publics doivent exclure les avis masqués.
2. Avec ADMIN, créer un profil Guide et un Circuit. Un touriste doit recevoir **403** s’il tente la même création.
3. Avec `demo-touriste`, réserver. Le prix vient de l’API Circuit ; la réservation apparaît dans son espace.
4. Avec `demo-autre`, essayer de lire cette réservation : accès refusé. Se déconnecter entre les deux comptes, ou utiliser des sessions de navigateur séparées.
5. Avec GUIDE et un profil associé, voir uniquement les circuits/réservations de ce guide.
6. Avec ADMIN, accepter ou refuser une demande ; vérifier qu’une décision incompatible avec le statut est rejetée.
7. Pour les avis, utiliser une réservation acceptée dont la visite est passée : vérifier propriété, notes et unicité. Les tests automatiques préparent ces dates ; ne modifiez pas des données réelles pour forcer l’éligibilité.

Pour tester directement une API, récupérez un access token de votre propre session locale dans les en-têtes réseau du navigateur. Gardez-le temporairement dans votre terminal, sans le coller dans un site externe ni dans Git :

```powershell
$env:ACCESS_TOKEN = '<votre access token local>'
curl.exe -i -H "Authorization: Bearer $env:ACCESS_TOKEN" http://localhost:8083/api/reservations/me
Remove-Item Env:ACCESS_TOKEN
```

Cet exemple demande un jeton USER. Utilisez le port réellement configuré. Sans l’en-tête, cette route doit répondre **401** ; avec un rôle non autorisé, **403**.

## 8. Intégrer sans écraser le travail des autres

Enregistrez d’abord votre travail sur votre branche habituelle. Ensuite, créez une branche d’intégration à partir de **votre service**, puis examinez la référence :

```powershell
git status
git fetch origin
git switch -c integration/auth-mon-service
git diff HEAD origin/auth-integration -- mon-service/
```

Remplacez `mon-service/` par `guide-service/`, `circuit-service/`, `reservation-service/` ou `avis-service/`. La dernière commande ne modifie rien : elle permet de voir les différences avant de reprendre les changements utiles.

Reprenez par petits groupes cohérents : dépendances + configuration JWT ; règles des routes ; propriété des données et migrations ; appels entre API ; tests. Ne copiez pas un `SecurityConfig` sans adapter ses routes, et ne remplacez pas tout votre service par le dossier de cette branche.

Si vous devez récupérer les dernières modifications de `main2`, faites-le séparément et résolvez les éventuels conflits avant la partie auth. Pour Guide/Avis, comparez particulièrement les migrations V1 à votre schéma existant. Pour Réservation, adaptez la V2 à votre historique Flyway au lieu de renuméroter une migration déjà appliquée.

Quand vos tests passent, ouvrez une Pull Request avec les routes protégées, les migrations prévues et les vérifications réalisées. Faites relire les échanges entre API par les deux responsables concernées, puis coordonnez l’activation du frontend privé avec celle des API protégées.

Cette branche utilise Keycloak `start-dev` et des adresses localhost pour les essais sur nos PC. Une mise en ligne demandera sa propre configuration HTTPS, ses vrais comptes et secrets, ses origines autorisées et ses sauvegardes.

## 9. Si quelque chose bloque

| Ce que vous voyez | Ce qu’il faut vérifier |
|---|---|
| 401 sur une route privée | Access token présent, non expiré, bonne signature, bon `iss`, `aud` contenant `guide-touristique-api` et `sub` présent |
| 403 | Rôle de realm attendu, mapping `ROLE_…`, puis propriété de la réservation / du profil |
| `/api/guides/me` renvoie 404 | Compte GUIDE non associé à un profil actif via `ownerSubject` |
| « Invalid redirect URI » | Adresse et port du frontend présents dans les redirect URIs du client Keycloak |
| Connexion indisponible | Keycloak prêt, port 8180 accessible et `auth-config.json` correct ; consulter `docker compose logs --tail 80 keycloak` |
| Impossible de charger les clés JWT dans Docker | Utiliser `keycloak:8080` pour les clés, sans changer arbitrairement le `iss` attendu |
| Erreur CORS | Origine exacte du frontend, en-tête Authorization et prérequêtes OPTIONS autorisés |
| 409 lors d’un avis ou d’un changement de statut | Conflit métier : visite pas encore éligible, avis déjà présent ou transition impossible |
| 503 entre services | Vérifier service cible, nom Docker, port et contrat JSON ; ne pas accepter l’opération sans vérification |
| Les nouveaux réglages JSON du realm n’apparaissent pas | Le realm existe déjà ; l’import ne l’écrase pas. Appliquer les changements ciblés dans la console |
| Mot de passe démo refusé après un nouveau lancement | Les comptes existants n’ont pas été recréés. Utiliser leurs accès actuels, sans supprimer les volumes |
| PostgreSQL refuse un nouveau compte service | Le volume est antérieur au nouvel `init.sql` ; organiser sa migration, pas sa suppression |

## Les liens utiles pour continuer

- [Contrats API détaillés](docs/api-contracts.md) : routes, champs JSON et permissions de cette branche.
- [Périmètre de la revue d’intégration](docs/auth-integration-review.md) : ce qui reste à valider ensemble.
- [Configuration Docker](docker-compose.yml) et [variables d’exemple](.env.example).
- [Provenance des photos](docs/landing-photos.md).

L’objectif est simple : une seule connexion pour l’utilisateur, et chaque service qui garde la responsabilité de ses propres données et permissions.
