# Contrats implémentés — branche circuit-service

HTTP/JSON, dates `YYYY-MM-DD`, montants MAD. Ports internes : Guide 8081, Circuit 8082, Réservation 8083, Avis 8084. Le frontend commun utilise `/api/...` ; le proxy transmet à chaque service.

Les routes protégées exigent `Authorization: Bearer <access_token>`. Chaque API vérifie signature, expiration, émetteur Keycloak et audience `guide-touristique-api`, puis les rôles de realm `USER`, `GUIDE`, `ADMIN`. Sans jeton : 401 ; sans permission : 403. Validation 400, absence 404, conflit 409, dépendance indisponible 503. La propriété dépend du `sub` signé, jamais d'un e-mail envoyé par le navigateur.

## Guide — Safa

| Méthode et chemin | Accès | Comportement |
|---|---|---|
| GET `/api/guides?ville=&langue=` | Public | Profils actifs sans coordonnées ni UUID de compte |
| GET `/api/guides/{id}` | Public | Profil public |
| GET `/api/guides/admin` | ADMIN | Profils complets |
| POST `/api/guides` | ADMIN | Créer (201) |
| PUT `/api/guides/{id}` | ADMIN | Modifier, associer un compte |
| DELETE `/api/guides/{id}` | ADMIN | Archiver et rendre indisponible (204) |
| GET, PUT `/api/guides/me` | GUIDE | Son profil uniquement |
| GET `/api/guides/me/circuits` | GUIDE | Appelle Circuit avec son guideId |

Corps ADMIN : `nom`, `ville`, `langues` (tableau non vide), `tarifParJour` (positif ou nul, deux décimales), `disponible`, `ownerSubject` (UUID Keycloak facultatif, unique), `email`, `telephone`. `/me` accepte les informations de profil **sans tarif ni UUID** : aucun changement d'association de compte par le guide.

## Circuit — Nada

| Méthode et chemin | Accès | Comportement |
|---|---|---|
| GET `/api/circuits?ville=&guideId=` | Public | Catalogue filtré |
| GET `/api/circuits/{id}` | Public | Détail |
| POST `/api/circuits` | ADMIN | Créer (201) |
| PUT `/api/circuits/{id}` | ADMIN | Modifier |
| DELETE `/api/circuits/{id}` | ADMIN | Archiver (204) |

Corps : `titre`, `description`, `ville`, `dureeHeures`, `prix`, **un seul `guideId`**. Circuit appelle GET Guide `/{id}` et exige un guide disponible. Aucun tableau de guides, aucune séance intermédiaire.

## Réservation — Mariem

| Méthode et chemin | Accès | Comportement |
|---|---|---|
| POST `/api/reservations` | USER | Créer pour le compte connecté (201) |
| GET `/api/reservations` | ADMIN | Toutes, y compris les anciennes sans propriétaire |
| GET `/api/reservations/me` | USER | Ses réservations |
| GET `/api/reservations/guide` | GUIDE | Réservations du guide identifié par GET Guide `/me`, même jeton |
| GET `/api/reservations/{id}` | Propriétaire USER ou ADMIN | Détail |
| PATCH `/api/reservations/{id}/annuler` | Propriétaire USER ou ADMIN | Annuler une demande en attente/acceptée, date non passée |
| PATCH `/api/reservations/{id}/accepter` ou `/refuser` | ADMIN | Décider une demande en attente, date non passée |
| GET `/api/reservations/{id}/eligibilite` | Propriétaire USER ou ADMIN | Références et droit de déposer un avis |

Création : `circuitId`, `dateVisite`, `nbPersonnes`, `nom`, `email`. Le serveur récupère le circuit, son guide et le prix via Circuit, vérifie le guide via Guide, calcule `prixTotal = prix × nbPersonnes`, conserve le `sub`. Le client ne choisit ni propriétaire, ni guide, ni prix total, ni statut.

Réponse : `id`, champs de création, `guideId`, `prixTotal`, `statut`, `dateCreation`. Statuts : `EN_ATTENTE`, `ACCEPTEE`, `REFUSEE`, `ANNULEE`. Le verrou optimiste protège les décisions concurrentes. Éligibilité : `{reservationId,circuitId,guideId,eligible,raison}`, vraie après la date d'une visite acceptée (Africa/Casablanca).

## Avis — Hajar

| Méthode et chemin | Accès | Comportement |
|---|---|---|
| GET `/api/avis?circuitId=&guideId=` | Public | Avis publiés |
| POST `/api/avis` | USER | Avis sur sa visite éligible (201) |
| GET `/api/avis/me` | USER | Ses avis |
| GET `/api/avis/admin` | ADMIN | Tous les avis |
| PATCH `/api/avis/{id}/publication` | ADMIN | `{ "publie": true/false }` |

Création : `{reservationId,noteGuide,noteCircuit,commentaire}`. Notes entières 1–5, commentaire non vide, maximum 2000 caractères. Avis transmet le jeton à Réservation pour vérifier propriété et éligibilité ; le serveur déduit circuit, guide et auteur. Une contrainte PostgreSQL garantit un avis par réservation.

Réponse : `id`, `reservationId`, `circuitId`, `guideId`, `auteur`, notes, commentaire, `dateCreation`, `publie`. Aucun identifiant de compte exposé.

Ces contrats remplacent les exemples du planning initial sur cette branche. Faire relire les intégrations Guide, Réservation et Avis par leurs responsables avant fusion dans main2.
