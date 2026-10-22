# 🗺️ Guide Touristique : Projet Web Services 2026

Application distribuée de guide touristique composée de **4 modules autonomes** qui communiquent entre eux via des **Web Services REST (JSON)**. Chaque module est conteneurisé avec **Docker**.

> Projet réalisé dans le cadre du module **Web Services**, Master d'excellence Ingénierie Logicielle, Faculté des Sciences d'Agadir.

---

## 👥 Équipe

| Membre | Rôle | Module | Technologie |
|---|---|---|---|
| Safa | Cheffe de projet - Développeuse | Guide |  |
| Nada | Développeuse | Circuit |  |
| Mariem | Développeuse | Réservation |  |
| Hajar | Développeuse | Avis |  |

---

## 🏗️ Architecture

```
                 ┌─────────────────────┐
                 │  PostgreSQL (db)    │
                 │  4 schémas privés   │
                 └──────────┬──────────┘
        ┌─────────────┬─────┴───────┬──────────────┐
┌───────┴──────┐ ┌────┴──────┐ ┌────┴────────┐ ┌───┴──────┐
│ guide-service│ │  circuit- │ │ reservation-│ │  avis-   │
│    :8001     │ │  service  │ │   service   │ │ service  │
│              │ │   :8002   │ │    :8003    │ │  :8004   │
└──────────────┘ └───────────┘ └─────────────┘ └──────────┘
        Communication entre modules : HTTP / REST / JSON
```

### Modules et dépendances

| Module | Port | Entité | Expose | Consomme |
|---|---|---|---|---|
| guide-service | 8001 | Guide | Gestion des guides | À définir |
| circuit-service | 8002 | Circuit | Gestion des circuits | guide-service |
| reservation-service | 8003 | Réservation | Gestion des réservations | circuit-service |
| avis-service | 8004 | Avis | Gestion des avis | circuit-service |
| db | 5432 interne / 15432 local | | Base PostgreSQL | |

---

## 🧰 Technologies

| Outil | Usage |
|---|---|
| REST / JSON | Communication entre les modules |
| Docker et Docker Compose | Conteneurisation et lancement de l'ensemble |
| PostgreSQL 16 | Base de données partagée |
| GitHub | Gestion du code source et travail collaboratif |
| Swagger / Postman | Documentation des API |

---

## 📁 Structure du dépôt

```
guide-touristique/
├── guide-service/          # Module Guide (Safa)
├── circuit-service/        # Module Circuit (Nada)
├── reservation-service/    # Module Réservation (Mariem)
├── avis-service/           # Module Avis (Hajar)
├── db/
│   └── init.sql            # Utilisateurs et schémas, sans tables métier
├── docs/                   # Contrats d'API, collections Postman
├── docker-compose.yml      # Lancement de toute l'application
├── compose.demo.yaml       # Simulateur Guide facultatif pour tester Circuit
├── .env.example            # Configuration locale à copier puis personnaliser
├── .gitignore
└── README.md
```

---

## 🚀 Installation et lancement

### Prérequis

- [Git](https://git-scm.com/)
- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (inclut Docker Compose)

### Lancer l'application

```bash
git clone https://github.com/safabelhoufrte/guideTouristique.git
cd guideTouristique
cp .env.example .env
# Remplacer les 5 mots de passe dans .env, puis :
docker compose up --build -d
```

Sur Windows PowerShell, utiliser `Copy-Item .env.example .env` à la place de `cp` si nécessaire.
Ne pas committer `.env`. Chaque membre conserve sa configuration locale.

Pour le moment, seuls PostgreSQL et Circuit sont implémentés dans cette branche.
Guide, Réservation et Avis seront ajoutés au Compose commun par leurs responsables.
Pour tester Circuit avec le simulateur Guide en attendant :

```bash
docker compose -f docker-compose.yml -f compose.demo.yaml up --build -d
python circuit-service/scripts/demo-client.py
```

Le simulateur utilise le port 8001 : l'arrêter avant de lancer le vrai service Guide.
Pour passer au vrai Guide, arrêter le mode démo avec `docker compose -f docker-compose.yml -f compose.demo.yaml down` sans `-v`, puis lancer le Compose normal avec le vrai service Guide ajouté.
Swagger Circuit : http://localhost:8002/swagger-ui/index.html.

### Accès aux services

| Service | URL |
|---|---|
| Guide | http://localhost:8001 |
| Circuit | http://localhost:8002 |
| Réservation | http://localhost:8003 |
| Avis | http://localhost:8004 |

### Commandes utiles

| Commande | Effet |
|---|---|
| `docker compose up -d` | Lancer en arrière-plan |
| `docker compose ps` | Voir les conteneurs actifs |
| `docker compose logs -f <service>` | Voir les logs d'un service |
| `docker compose down` | Arrêter l'application |
| `docker compose down -v` | Réinitialisation volontaire uniquement : supprime toutes les données du volume |

---

## 🗄️ Base de données

- **SGBD** : PostgreSQL 16
- **Base** : `guide_touristique` (une seule base partagée entre les 4 modules)
- **Initialisation** : `db/init.sql` crée les utilisateurs et schémas au premier lancement d'un volume neuf.
- **Tables** : chaque service crée et fait évoluer ses tables avec ses propres migrations. Circuit utilise Flyway.

| Schéma / utilisateur PostgreSQL | Module propriétaire | Mot de passe dans `.env` |
|---|---|---|
| `guide` | guide-service | `GUIDE_DB_PASSWORD` |
| `circuit` | circuit-service | `CIRCUIT_DB_PASSWORD` |
| `reservation` | reservation-service | `RESERVATION_DB_PASSWORD` |
| `avis` | avis-service | `AVIS_DB_PASSWORD` |

Dans Docker : hôte `db`, port `5432`, base `guide_touristique` par défaut.
Depuis un programme local : hôte `localhost`, port `15432` (configurable par `DB_HOST_PORT`).
Chaque service reçoit seulement son propre utilisateur et son mot de passe, jamais le compte administrateur `postgres`.
Son schéma est aussi son `search_path` par défaut ; les outils de migration des différents langages peuvent donc gérer leurs tables séparément.

### ⚠️ Règles d'utilisation

1. Chaque module lit et écrit **uniquement dans son propre schéma**. PostgreSQL interdit l'accès aux autres schémas.
2. Pour obtenir les données d'un autre module, on appelle **son API REST**.
3. Pas de clé étrangère entre les tables de modules différents.
4. Pour modifier les tables, ajouter une migration dans le service concerné. Pour Circuit : `V2__description.sql`, puis `V3__...sql`, dans `circuit-service/src/main/resources/db/migration/circuit/`. Ne pas modifier une migration déjà appliquée.
5. Ne pas utiliser `docker compose down -v` pour une modification normale : cette commande supprime les données. Un changement de `init.sql` ou des mots de passe d'initialisation ne modifie pas un volume existant ; une mise à jour administrative explicite est alors nécessaire.

### Se connecter à la base

```bash
docker compose exec db psql -U postgres -d guide_touristique
```

Cette console est réservée à l'administration locale. Les applications utilisent leur compte limité.
Commandes `psql` : `\dt` (lister les tables), `\q` (quitter).

---

## 📖 Documentation des API

Les contrats d'API (endpoints, paramètres, formats JSON, exemples) sont dans le dossier [`docs/`](docs/).

Convention commune :

| Code HTTP | Signification |
|---|---|
| 200 | Succès |
| 201 | Ressource créée |
| 400 | Données invalides |
| 404 | Ressource introuvable |
| 500 | Erreur serveur |

Les noms de champs JSON sont en minuscules avec underscore (exemple : `guide_id`, `circuit_id`).

---

## 🤝 Organisation du travail

### Workflow Git

```
git checkout main && git pull          # récupérer la dernière version
git checkout -b feature/<module>       # créer sa branche
# ... développement ...
git add . && git commit -m "message"   # enregistrer
git push -u origin feature/<module>    # envoyer sur GitHub
# puis ouvrir une Pull Request vers main
```

### Règles

- Ne jamais committer directement sur `main`.
- Une branche par module : `feature/guide`, `feature/circuit`, `feature/reservation`, `feature/avis`.
- Chaque membre travaille uniquement dans le dossier de son module.
- Toute pull request est relue et validée par la cheffe de projet.
- Messages de commit clairs (exemple : `Ajout du CRUD guides`).
- Ne jamais envoyer sur GitHub : `.env`, `vendor/`, `node_modules/`, `target/`.

### Suivi

- Point d'avancement à chaque séance.
- Tâches suivies dans l'onglet **Issues / Projects** de GitHub.

---



📌 **Date limite de livraison : 20/11/2026** · Soutenances à partir du 26/11/2026

---

## 📦 Livrables

- [ ] Dépôt GitHub (code, Dockerfiles, Docker Compose, instructions)
- [ ] Documentation des Web Services (endpoints, paramètres, exemples)
- [ ] Présentation finale avec démonstration
