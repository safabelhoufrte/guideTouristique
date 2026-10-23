# Contrats d'API – Guide Touristique

## Conventions
- JSON, champs en camelCase, IDs en Long, dates ISO 8601
- Format d'erreur : { timestamp, status, error, message, path }
- Swagger de chaque service : http://localhost:<port>/swagger-ui.html

## Qui appelle qui
- circuit-service  →  guide-service (GET /api/guides/{id})
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
  "guideId": 1
}
```

### Endpoints
| Méthode | URL | Description | Réponse |
|---|---|---|---|
| GET | /api/circuits | Liste (filtres : ?ville=, ?guideId=) | 200 |
| GET | /api/circuits/{id} | Détail | 200 / 404 |
| POST | /api/circuits | Créer (vérifie que guideId existe) | 201 / 400 |
| PUT | /api/circuits/{id} | Modifier | 200 / 404 |
| DELETE | /api/circuits/{id} | Supprimer | 204 / 404 |

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