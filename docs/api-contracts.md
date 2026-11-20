# Contrats d'API – Guide Touristique

## Conventions
- JSON, champs en camelCase, IDs en Long, dates ISO 8601
- Format d'erreur : { timestamp, status, error, message, path }
- Swagger de chaque service : http://localhost:<port>/swagger-ui.html

## Qui appelle qui
- circuit-service  →  guide-service (GET /api/guides/{id} ; GET /api/guides/me avec le token du guide connecté)
- reservation-service  →  guide-service et circuit-service
- avis-service  →  guide-service et circuit-service

---

## 1. Guide Service (port 8081) – Safa

### Modèle Guide
```json
{
  "id": 1,
  "nom": "Benali",
  "prenom": "Youssef",
  "ville": "Agadir",
  "langues": ["français", "anglais", "arabe"],
  "email": "youssef@example.com",
  "telephone": "0600000000",
  "tarifParJour": 400.0,
  "disponible": true
}
```

### Endpoints
| Méthode | URL | Description | Réponse |
|---|---|---|---|
| GET | /api/guides | Liste (filtres : ?ville=, ?langue=) | 200 + liste |
| GET | /api/guides/{id} | Détail d'un guide | 200 / 404 |
| POST | /api/guides | Créer un guide | 201 / 400 |
| PUT | /api/guides/{id} | Modifier | 200 / 404 |
| DELETE | /api/guides/{id} | Supprimer | 204 / 404 |
| GET | /api/guides/{id}/disponibilite | Disponibilité | 200 |

---

## 2. Circuit Service (port 8082) – Nada

### Modèle Circuit
```json
{
  "id": 1,
  "titre": "Médina et Kasbah",
  "description": "Visite guidée de la vieille ville",
  "ville": "Agadir",
  "dureeHeures": 4,
  "prix": 250.0,
  "guideId": 1,
  "image": "/api/circuits/1/image"
}
```

`image` est un lien vers la photo, ou `null` si le circuit n'en a pas. Il
s'utilise tel quel dans `<img [src]>`. Les listes restent légères : la photo
n'est jamais incluse dans le JSON.

En écriture (POST et PUT), `image` accepte :
- une data URL `data:image/(jpeg|png|webp);base64,…` (1 500 000 caractères au plus) : nouvelle photo ;
- `""` : retire la photo ;
- `null` ou champ absent : en PUT, garde la photo actuelle ; en POST, pas de photo.

Tout autre format → 400.

### Endpoints
| Méthode | URL | Description | Réponse |
|---|---|---|---|
| GET | /api/circuits | Liste (filtres : ?ville=, ?guideId=) | 200 |
| GET | /api/circuits/{id} | Détail | 200 / 404 |
| GET | /api/circuits/{id}/image | Photo (octets, `Content-Type` image/jpeg, png ou webp ; `ETag`, 304 si inchangée) | 200 / 304 / 404 |
| POST | /api/circuits | ADMIN ou GUIDE — créer (vérifie que guideId existe) | 201 / 400 / 401 / 403 / 503 |
| PUT | /api/circuits/{id} | ADMIN ou GUIDE — modifier | 200 / 404 / 401 / 403 / 503 |
| DELETE | /api/circuits/{id} | ADMIN ou GUIDE — archiver sans effacer l'historique | 204 / 404 / 401 / 403 / 503 |

Les trois routes GET restent publiques. La photo d'un circuit archivé renvoie 404. Les écritures exigent un access token signé :
émetteur Keycloak attendu, audience `guide-touristique-api`, sujet non vide,
dates valides et rôle `ADMIN` ou `GUIDE` dans `realm_access.roles`. USER ne peut
pas écrire. Voir [la configuration Circuit](circuit-integration.md).

Règles du rôle GUIDE (contrôlées par le serveur, jamais par le frontend) :
- Circuit identifie le guide connecté en appelant `GET /api/guides/me` du service
  Guide avec le même token. Réponse attendue : au moins `{ "id": <long> }`.
  401/403/404 de Guide → 403 « Aucun profil guide lié à ce compte » ; autre erreur → 503.
- Un guide ne crée un circuit que pour lui-même (`guideId` = son id), sinon 403.
- Un guide ne modifie ou n'archive que ses propres circuits, et ne peut pas
  les transférer à un autre guide, sinon 403.
- Pour afficher « Mes circuits » : `GET /api/circuits?guideId=<id de /api/guides/me>`.

Consomme : GET guide-service /api/guides/{id}

---

## 3. Réservation Service (port 8083) – Mariem

### Modèle Reservation
```json
{
  "id": 1,
  "circuitId": 1,
  "guideId": 1,
  "nomClient": "Sara Alami",
  "emailClient": "sara@example.com",
  "dateCircuit": "2026-12-10",
  "nombrePersonnes": 3,
  "statut": "EN_ATTENTE"
}
```
Statuts possibles : EN_ATTENTE, CONFIRMEE, ANNULEE

### Endpoints
| Méthode | URL | Description | Réponse |
|---|---|---|---|
| GET | /api/reservations | Liste (?statut=, ?circuitId=) | 200 |
| GET | /api/reservations/{id} | Détail | 200 / 404 |
| POST | /api/reservations | Créer (vérifie circuit et guide) | 201 / 400 / 503 |
| PUT | /api/reservations/{id}/statut | Changer le statut | 200 / 404 |
| DELETE | /api/reservations/{id} | Annuler | 204 / 404 |

Consomme : guide-service et circuit-service (GET par id)

---

## 4. Avis Service (port 8084) – Hajar

### Modèle Avis
```json
{
  "id": 1,
  "cibleType": "CIRCUIT",
  "cibleId": 1,
  "auteur": "Sara Alami",
  "note": 5,
  "commentaire": "Excellente visite !",
  "dateCreation": "2026-11-05T14:30:00"
}
```
cibleType : GUIDE ou CIRCUIT. note : entier de 1 à 5.

### Endpoints
| Méthode | URL | Description | Réponse |
|---|---|---|---|
| GET | /api/avis | Liste (?cibleType=, ?cibleId=) | 200 |
| GET | /api/avis/{id} | Détail | 200 / 404 |
| POST | /api/avis | Créer (vérifie que la cible existe) | 201 / 400 / 503 |
| DELETE | /api/avis/{id} | Supprimer | 204 / 404 |
| GET | /api/avis/moyenne?cibleType=&cibleId= | Note moyenne | 200 |

Consomme : guide-service et circuit-service (GET par id)
