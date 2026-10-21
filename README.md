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
                 │  base partagée      │
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
| db | 5432 | | Base PostgreSQL | |

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
│   └── init.sql            # Création des tables
├── docs/                   # Contrats d'API, collections Postman
├── docker-compose.yml      # Lancement de toute l'application
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
docker compose up --build
```

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
| `docker compose down -v` | Arrêter et réinitialiser la base de données |

---

## 🗄️ Base de données

- **SGBD** : PostgreSQL 16
- **Base** : `guide_touristique` (une seule base partagée entre les 4 modules)
- **Création des tables** : automatique au premier lancement via `db/init.sql`

| Table | Module propriétaire |
|---|---|
| `guides` | guide-service |
| `circuits` | circuit-service |
| `reservations` | reservation-service |
| `avis` | avis-service |

### ⚠️ Règles d'utilisation

1. Chaque module lit et écrit **uniquement dans sa propre table**.
2. Pour obtenir les données d'un autre module, on appelle **son API REST**.
3. Pas de clé étrangère entre les tables de modules différents.
4. Modifier `init.sql` puis lancer `docker compose down -v` pour que les changements soient pris en compte.

### Se connecter à la base

```bash
docker compose exec db psql -U admin -d guide_touristique
```
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