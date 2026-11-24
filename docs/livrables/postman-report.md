# Rapport d'Exécution des Tests Postman / Newman — avis-service

**Date :** 2026-11-23  
**Branche :** `avis-integration` (PR #26 vers `main2`)  
**Service testé :** `avis-service` (port `8084`)  
**Collection Postman :** `docs/livrables/avis-service.postman_collection.json` (Format v2.1)  
**Outil d'exécution automatisée :** Newman v6.2.3  

---

## 1. Contexte d'authentification et Configuration Keycloak

- **Client ID public :** `guide-touristique-web` (extrait de `infra/keycloak/guide-touristique-realm.json`)
- **Type de client :** Public (`publicClient: true`)
- **Direct Access Grants (grant_type=password) :** Actif dans l'environnement Keycloak local
- **Endpoint Token :** `POST http://localhost:8180/realms/guide-touristique/protocol/openid-connect/token`
- **Utilisateurs utilisés :**
  - `demo-touriste` : propriétaire de la réservation #1 (statut `ACCEPTEE`, date passée)
  - `demo-autre` : autre utilisateur authentifié sans droit sur la réservation #1

---

## 2. Résultats des 9 Scénarios de Test

| N° | Scénario / Requête | Méthode & URL | Authentification | Code attendu | Code obtenu | Résultat | Remarques / Validation |
|---|---|---|---|:---:|:---:|:---:|---|
| **1** | Consultation des avis publics | `GET /api/avis` | Aucune (public) | `200` | `200 OK` | **PASS** | Retourne la liste JSON des avis publiés |
| **2** | Vérification de l'état de santé | `GET /actuator/health` | Aucune (public) | `200` | `200 OK` | **PASS** | Statut `{"status": "UP"}` |
| **3** | Éligibilité sans authentification | `GET /api/avis/eligibilite` | Aucune | `401` | `401 Unauthorized` | **PASS** | Accès protégé refusé sans token |
| **4** | Éligibilité touriste autorisé | `GET /api/avis/eligibilite` | Bearer `demo-touriste` | `200` | `200 OK` | **PASS** | `{"eligible": true, "reservations": [...]}` |
| **5** | Création d'avis sans authentification | `POST /api/avis` | Aucune | `401` | `401 Unauthorized` | **PASS** | Création refusée sans jeton valide |
| **6** | Création d'avis valide (réservation #1) | `POST /api/avis` | Bearer `demo-touriste` | `201` | `201 Created` | **PASS** | Avis créé avec succès (`reservationId: 1`, `noteGuide: 5`, `noteCircuit: 4`) |
| **7** | Création en double (même réservation) | `POST /api/avis` | Bearer `demo-touriste` | `409` | `409 Conflict` | **PASS** | Violation d'unicité : un seul avis par réservation |
| **8** | Création par un utilisateur non propriétaire | `POST /api/avis` | Bearer `demo-autre` | `403` | `403 Forbidden` | **PASS** | La réservation n'appartient pas au demandeur |
| **9** | Création avec note guide invalide (> 5) | `POST /api/avis` | Bearer `demo-touriste` | `400` | `400 Bad Request` | **PASS** | Validation bean validation (`@Max(5)` sur `noteGuide`) |

---

## 3. Synthèse de l'Exécution Newman

```text
Collection : avis-service
Total des requêtes exécutées : 11 (2 obtentions de token + 9 scénarios de test)
Total des assertions : 11
Échecs : 0
Durée totale du run : 2.5 s
Taux de réussite : 100%
```

Toutes les requêtes et assertions ont été validées conformément aux exigences métier et de sécurité.
