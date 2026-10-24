# 🧭 Guide Touristique

Application web distribuée de **guide touristique**, composée de 4 modules métier autonomes qui communiquent entre eux via des **Web Services REST (JSON)**.

> Projet Web Services 2026 – Développement collaboratif basé sur les Web Services
> **Deadline de livraison : 20/11/2026** · **Soutenances : à partir du 26/11/2026**

---

## 📑 Table des matières

1. [Présentation du projet](#1-présentation-du-projet)
2. [L'équipe et la répartition](#2-léquipe-et-la-répartition)
3. [Technologies utilisées](#3-technologies-utilisées)
4. [Architecture](#4-architecture)
5. [Structure du dépôt](#5-structure-du-dépôt)
6. [Prérequis et installation](#6-prérequis-et-installation)
7. [Lancer l'application avec Docker](#7-lancer-lapplication-avec-docker)
8. [Stratégie de branches Git](#8-stratégie-de-branches-git)
9. [Workflow de travail quotidien](#9-workflow-de-travail-quotidien)
10. [Pull Requests et intégration](#10-pull-requests-et-intégration)
11. [Conventions de l'équipe](#11-conventions-de-léquipe)
12. [Documentation des API (Swagger)](#12-documentation-des-api-swagger)
13. [Planning](#13-planning)
14. [Aide-mémoire des commandes](#14-aide-mémoire-des-commandes)
15. [Problèmes fréquents](#15-problèmes-fréquents)
16. [Checklist de livraison](#16-checklist-de-livraison)

---

## 1. Présentation du projet

Le projet consiste à concevoir une application distribuée de guide touristique. Chaque module est **indépendant** : il a son propre code, sa propre base de données, son propre Dockerfile, et il **expose** et **consomme** des Web Services REST.

| Module | Rôle |
|---|---|
| **Guide** | Gestion des guides touristiques (profil, langues, ville, disponibilité) |
| **Circuit** | Gestion des circuits touristiques, associés à un guide |
| **Réservation** | Réservation d'un circuit auprès d'un guide |
| **Avis** | Notes et commentaires sur les guides et les circuits |

## 2. L'équipe et la répartition

| Membre | Rôle | Module | Branche | Port |
|---|---|---|---|---|
| **Safa** | **Cheffe de projet** + développeuse | Guide | `guide` | 8081 |
| **Nada** | Développeuse | Circuit | `circuit` | 8082 |
| **Mariem** | Développeuse | Réservation | `reservation` | 8083 |
| **Hajar** | Développeuse | Avis | `avis` | 8084 |


## 3. Technologies utilisées

| Couche | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 3 (Spring Web, Spring Data JPA) |
| Frontend | Angular |
| Base de données | PostgreSQL 16 (une instance, **une base par service**) |
| Conteneurisation | Docker et Docker Compose |
| Documentation API | Swagger / OpenAPI (springdoc-openapi) |
| Gestion de code | Git et GitHub (branches, Pull Requests, Projects) |

## 4. Architecture

```
                      ┌──────────────────────┐
                      │   Frontend Angular   │  :4200
                      └──────────┬───────────┘
          ┌───────────────┬──────┴────────┬───────────────┐
          ▼               ▼               ▼               ▼
  ┌──────────────┐ ┌──────────────┐ ┌───────────────┐ ┌──────────────┐
  │ guide-service│ │circuit-service│ │reservation-svc│ │  avis-service│
  │    :8081     │ │    :8082     │ │    :8083      │ │    :8084     │
  └──────┬───────┘ └──────┬───────┘ └───────┬───────┘ └──────┬───────┘
         │                │                 │                │
         ▼                ▼                 ▼                ▼
     guide_db        circuit_db      reservation_db       avis_db
                    └──────────── PostgreSQL :5432 ────────────┘
```

### Qui appelle qui (communication REST entre services)

| Service | Expose | Consomme |
|---|---|---|
| guide-service | Liste, détail, disponibilité des guides | – |
| circuit-service | Circuits | guide-service (`GET /api/guides/{id}`) |
| reservation-service | Réservations | guide-service et circuit-service |
| avis-service | Avis et note moyenne | guide-service et circuit-service |

> Entre conteneurs, les services s'appellent par **leur nom Docker** (ex. `http://guide-service:8081`), jamais par `localhost`.

## 5. Structure du dépôt

```
guideTouristique/
├── guide-service/          # Safa
│   ├── Dockerfile
│   └── .dockerignore
├── circuit-service/        # Nada
├── reservation-service/    # Mariem
├── avis-service/           # Hajar
├── frontend/               # Angular (Safa)
├── db/
│   └── init.sql            # Création des 4 bases PostgreSQL
├── docs/
│   └── api-contracts.md    # Contrats d'API (à respecter !)
├── docker-compose.yml
├── .env.example
├── .gitignore
└── README.md
```

## 6. Prérequis et installation

À installer sur chaque PC :

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (doit être **lancé** avant toute commande Docker)
- JDK 21 et Maven (ou l'IDE IntelliJ / VS Code)
- Node.js 20+ et Angular CLI (pour le frontend)

Vérification :

```bash
git --version
docker --version
docker compose version
java -version
```

### Première installation (une seule fois)

```bash
# 1. Cloner le dépôt
git clone https://github.com/<compte-github>/guideTouristique.git
cd guideTouristique

# 2. Créer son fichier d'environnement local (NE JAMAIS le commiter)
cp .env.example .env

# 3. Récupérer les branches distantes
git fetch --all
```

## 7. Lancer l'application avec Docker

```bash
# Tout construire et lancer
docker compose up --build

# Ou en arrière-plan
docker compose up -d --build
```

| Élément | URL |
|---|---|
| Guide service | http://localhost:8081 |
| Circuit service | http://localhost:8082 |
| Réservation service | http://localhost:8083 |
| Avis service | http://localhost:8084 |
| Frontend | http://localhost:4200 |
| PostgreSQL | `localhost:15433` (configurable) |

### Tester uniquement la base de données

```bash
docker compose up -d postgres
docker compose ps          # doit afficher "healthy"
docker compose exec postgres psql -U postgres -c "\l"
```

### Ajouter son service au `docker-compose.yml`

Chaque développeuse ajoute **son bloc** en s'inspirant de `guide-service`. Exemple pour `circuit-service` :

```yaml
  circuit-service:
    build: ./circuit-service
    ports:
      - "8082:8082"
    environment:
      SERVER_PORT: 8082
      DB_URL: jdbc:postgresql://postgres:5432/circuit_db
      DB_USER: circuit
      DB_PASSWORD: ${CIRCUIT_DB_PASSWORD}
      GUIDE_SERVICE_URL: http://guide-service:8081
    depends_on:
      postgres:
        condition: service_healthy
```

> ⚠️ Le fichier est partagé : pour éviter les conflits, chacune ajoute son bloc **à la fin de la section `services:`**, juste avant `volumes:`, et prévient la cheffe de projet.

### Configuration Spring Boot (`application.properties`)

```properties
server.port=${SERVER_PORT:8081}
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

Les URLs des autres services se lisent aussi via une variable, par exemple :

```properties
guide.service.url=${GUIDE_SERVICE_URL:http://localhost:8081}
```

### État vérifié de Circuit sur cette branche

- Circuit respecte `docs/api-contracts.md` : Java 21, port 8082, `/api/circuits`, JSON camelCase et base `circuit_db`. Le dépôt JDBC conserve la séparation contrôleur/service/repository et utilise Flyway.
- La branche `guide` contient actuellement le squelette Docker et les contrats, sans `pom.xml` ni code Guide. Un test avec le vrai Guide reste à effectuer après son implémentation.
- Pour tester maintenant : `docker compose -f docker-compose.yml -f compose.demo.yaml up --build -d`, puis `python circuit-service/scripts/demo-client.py`. Le mode démo remplace seulement le build Guide par un simulateur Python.
- Dans `.env`, définir `DB_USER`, `DB_PASSWORD` administrateur et les quatre mots de passe `GUIDE_DB_PASSWORD`, `CIRCUIT_DB_PASSWORD`, `RESERVATION_DB_PASSWORD`, `AVIS_DB_PASSWORD`. Chaque application reçoit uniquement son propre compte limité.
- PostgreSQL est accessible localement sur `localhost:15433` par défaut (`DB_HOST_PORT`), et sur `postgres:5432` entre conteneurs. Les anciennes bases et volumes locaux ne sont pas supprimés ni importés automatiquement.
- Si le port local 8082 est occupé, régler `CIRCUIT_HOST_PORT` dans `.env` (par exemple 18092). Le port interne reste 8082 ; utiliser alors `python circuit-service/scripts/demo-client.py --url http://localhost:18092` et Swagger sur ce même port local.
- `init.sql` crée les quatre bases et comptes uniquement pour un volume neuf. Les évolutions des tables Circuit passent par de nouvelles migrations Flyway, sans modifier V1 déjà appliquée ni utiliser `down -v`. Pour un volume existant provenant de main2, un administrateur doit créer les comptes et attribuer les bases avant de lancer cette configuration.
- Les champs ajoutés par V2 restent null pour les anciens enregistrements dont ces informations sont inconnues ; ils deviennent obligatoires dans les nouvelles requêtes.

## 8. Stratégie de branches Git

```
main            → version finale et stable (celle que le prof voit)
  └── main2     → branche d'intégration et de tests
        ├── guide         (Safa)
        ├── circuit       (Nada)
        ├── reservation   (Mariem)
        └── avis          (Hajar)
```

| Branche | Rôle | Qui peut la modifier ? |
|---|---|---|
| `main` | Version finale, testée et validée | Uniquement via PR depuis `main2` (Safa) |
| `main2` | Intégration : on y fusionne les services pour vérifier que tout marche ensemble | Uniquement via PR depuis une branche de service |
| `guide`, `circuit`, `reservation`, `avis` | Développement d'un service | La propriétaire de la branche |

**Règles d'or :**

1. ❌ Personne ne pousse directement sur `main` ni sur `main2` (sauf Safa pour la config commune).
2. ✅ Chacune travaille **uniquement dans le dossier de son service**.
3. ✅ On fusionne d'abord vers `main2`, on teste, et **seulement ensuite** `main2` vers `main`.
4. ✅ Si on doit modifier un fichier commun (`docker-compose.yml`, `docs/`), on prévient Safa.

### Créer sa branche (une seule fois)

```bash
git checkout main2
git pull origin main2

# remplacer <service> par : circuit, reservation ou avis
git checkout -b <service>
git push -u origin <service>
```

## 9. Workflow de travail quotidien

### Début de journée : se mettre à jour

```bash
git checkout <ma-branche>
git pull origin <ma-branche>
git merge main2        # récupérer les nouveautés communes
```

### Pendant le travail : commiter souvent

```bash
git status                       # voir ce qui a changé
git add .
git commit -m "feat: ajout endpoint GET /api/guides (#3)"
git push origin <ma-branche>
```

### Convention des messages de commit

| Préfixe | Utilisation | Exemple |
|---|---|---|
| `feat:` | nouvelle fonctionnalité | `feat: ajout entité Guide (#2)` |
| `fix:` | correction de bug | `fix: erreur 404 sur GET /guides/{id}` |
| `docs:` | documentation | `docs: mise à jour du contrat d'API` |
| `chore:` | config, Docker, outils | `chore: ajout Dockerfile guide-service` |
| `test:` | tests | `test: tests du CRUD guide` |
| `refactor:` | amélioration du code | `refactor: séparation service/contrôleur` |

> Le numéro `(#3)` renvoie à l'issue GitHub correspondante. Écrire `closes #3` dans une PR ferme automatiquement l'issue au merge.

### Avant d'ouvrir une Pull Request (checklist)

```bash
git checkout <ma-branche>
git pull origin main2          # intégrer les dernières modifications
# → résoudre les conflits s'il y en a
docker compose up --build      # vérifier que mon service démarre
```

- [ ] Mon service démarre sans erreur dans Docker
- [ ] Mes endpoints respectent `docs/api-contracts.md`
- [ ] Swagger est accessible
- [ ] Aucun fichier `.env`, `target/` ou `node_modules/` dans le commit

## 10. Pull Requests et intégration

### A. Service → `main2` (par chaque développeuse)

1. `git push origin <ma-branche>`
2. Sur GitHub : **Pull requests → New pull request**
3. **base : `main2`** ← **compare : `<ma-branche>`**
4. Titre clair, description courte, `closes #numéro`, puis **Create pull request**
5. Safa relit, teste, puis clique sur **Merge pull request**

### B. `main2` → `main` (par Safa, une fois tout validé)

1. Sur `main2` : `docker compose up --build` et test de **tous** les services.
2. Pull request : **base `main`** ← **compare `main2`**.
3. Merge, puis vérification finale sur `main`.

Équivalent en ligne de commande (si les règles de protection le permettent) :

```bash
# Safa uniquement
git checkout main2
git pull origin main2
git checkout main
git pull origin main
git merge main2
git push origin main
```

### Pousser sa branche `guide` (Safa)

```bash
git checkout guide
git add .
git commit -m "feat: description du changement"
git push origin guide
```

### Pousser le squelette ou la config commune sur `main2` (Safa)

```bash
git checkout main2
git pull origin main2
git add .
git commit -m "chore: description du changement"
git push origin main2

# puis remettre les nouveautés dans guide
git checkout guide
git merge main2
```

### Résoudre un conflit

Si Git affiche `CONFLICT`, ouvrir le fichier concerné : les zones en conflit sont entourées de `<<<<<<<`, `=======` et `>>>>>>>`. Garder le bon contenu (en discuter avec la personne concernée), supprimer ces marqueurs, puis :

```bash
git add .
git commit -m "fix: résolution de conflit"
git push
```

## 11. Conventions de l'équipe

| Sujet | Règle |
|---|---|
| Préfixes d'URL | `/api/guides`, `/api/circuits`, `/api/reservations`, `/api/avis` |
| Format des données | JSON |
| Noms de champs JSON | camelCase (`dateCreation`) |
| Identifiants | `Long` |
| Dates | ISO 8601 (`2026-11-05` ou `2026-11-05T14:30:00`) |
| Ports | 8081 guide · 8082 circuit · 8083 réservation · 8084 avis |
| Langue du code | À fixer ensemble (tout en français ou tout en anglais) |

**Codes HTTP :** `200` OK · `201` créé · `204` supprimé · `400` données invalides · `404` introuvable · `503` autre service indisponible.

**Format d'erreur commun à tous les services :**

```json
{
  "timestamp": "2026-11-05T14:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Guide avec id 12 introuvable",
  "path": "/api/guides/12"
}
```

**Contrats d'API :** voir [`docs/api-contracts.md`](docs/api-contracts.md). Aucun endpoint ne change sans que le fichier soit modifié d'abord et que l'équipe soit prévenue.

**CORS :** chaque service autorise `http://localhost:4200` (frontend Angular).

**Appels entre services :** utiliser `RestTemplate` ou `WebClient` et gérer le cas où l'autre service est indisponible (réponse `503`).

## 12. Documentation des API (Swagger)

Dépendance Maven à ajouter dans chaque service :

```xml
<dependency>
  <groupId>org.springdoc</groupId>
  <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
  <version>2.6.0</version>
</dependency>
```

| Service | Swagger UI |
|---|---|
| Guide | http://localhost:8081/swagger-ui.html |
| Circuit | http://localhost:8082/swagger-ui.html |
| Réservation | http://localhost:8083/swagger-ui.html |
| Avis | http://localhost:8084/swagger-ui.html |

## 13. Planning

| Période | Objectifs |
|---|---|
| **S1 (23/10 – 30/10)** | Squelette Git/Docker, contrats d'API, création des projets Spring Boot, entités et premiers endpoints |
| **S2 (31/10 – 06/11)** | CRUD terminés, Swagger, Dockerfile fonctionnel, squelette Angular |
| **S3 (07/11 – 13/11)** | Appels REST entre services, premières PR vers `main2`, frontend connecté aux API |
| **S4 (14/11 – 20/11)** | Tests d'intégration sur `main2`, corrections, documentation, merge vers `main`. **Gel du code le 18/11** |
| **À partir du 26/11** | Soutenances (démo de l'application et du travail de chaque membre) |

**Suivi :** tableau GitHub Projects (Todo / In Progress / Done) + points d'équipe de 15 minutes deux fois par semaine (fait / à faire / blocages).

## 14. Aide-mémoire des commandes

### Git

| Commande | Rôle |
|---|---|
| `git branch` | Voir les branches (l'étoile `*` = branche actuelle) |
| `git checkout <branche>` | Changer de branche |
| `git checkout -b <branche>` | Créer et aller sur une nouvelle branche |
| `git status` | Voir l'état des fichiers |
| `git add .` | Préparer tous les fichiers modifiés |
| `git commit -m "message"` | Enregistrer les changements |
| `git push origin <branche>` | Envoyer sur GitHub |
| `git pull origin <branche>` | Récupérer depuis GitHub |
| `git merge main2` | Fusionner main2 dans la branche actuelle |
| `git log --oneline` | Historique résumé |
| `git stash` / `git stash pop` | Mettre de côté / récupérer des modifications non commitées |

### Docker

| Commande | Rôle |
|---|---|
| `docker compose up --build` | Construire et lancer tout |
| `docker compose up -d` | Lancer en arrière-plan |
| `docker compose up -d --build <service>` | Reconstruire et relancer un seul service |
| `docker compose down` | Arrêter tout |
| `docker compose down -v` | Arrêter et **effacer les données** de la base |
| `docker compose ps` | Voir les conteneurs actifs |
| `docker compose logs -f <service>` | Suivre les logs d'un service |
| `docker compose exec postgres psql -U postgres -d <base>` | Ouvrir la console PostgreSQL |

Noms des services : `postgres`, `guide-service`, `circuit-service`, `reservation-service`, `avis-service`, `frontend`.

## 15. Problèmes fréquents

| Problème | Solution |
|---|---|
| `Cannot connect to the Docker daemon` | Lancer Docker Desktop et attendre qu'il soit prêt |
| `port is already allocated` | Un autre programme utilise le port : changer le port de gauche (ex. `"5433:5432"`) |
| `Connection refused` vers PostgreSQL | Utiliser `postgres` et non `localhost` dans `DB_URL` |
| `database "xxx_db" does not exist` | Créer la base explicitement avec le compte administrateur, sans supprimer le volume existant |
| `pom.xml not found` au build | Le projet Spring Boot doit être à la racine du dossier du service |
| `Your local changes would be overwritten` | Commiter (`git add .` puis `git commit`) ou `git stash` avant de changer de branche |
| Erreur CORS dans le navigateur | Autoriser `http://localhost:4200` dans le service Java |
| Push refusé sur `main` / `main2` | Normal : passer par une Pull Request |
| Fichier `.env` apparu sur GitHub | Prévenir Safa immédiatement (changer les mots de passe, retirer le fichier du dépôt) |

## 16. Checklist de livraison

- [ ] Les 4 services démarrent avec `docker compose up --build`
- [ ] Chaque service **expose** et **consomme** au moins un Web Service
- [ ] Chaque service a son Dockerfile
- [ ] Swagger accessible pour chaque service
- [ ] `docs/api-contracts.md` à jour
- [ ] Données de démo chargées au démarrage
- [ ] Frontend Angular connecté aux services
- [ ] README complet et à jour
- [ ] Contributions de chaque membre visibles (commits, PR, issues)
- [ ] Code final fusionné dans `main`
- [ ] Prof ajouté comme collaborateur (si le dépôt est privé)
- [ ] Présentation finale et schéma d'architecture prêts

---

👩‍💻 **Équipe :** Safa (cheffe de projet, Guide) · Nada (Circuit) · Mariem (Réservation) · Hajar (Avis)
