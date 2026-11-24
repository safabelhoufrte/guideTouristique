# Rapport de Build et Tests

**Date :** 2026-11-23  
**Branche :** `avis-integration`  
**Contexte :** PR #26 vers `main2`  

---

## 1. avis-service — Tests unitaires & intégration

- **Commande :**
  ```bash
  cd avis-service ; mvn -B -ntp verify
  ```
- **Résultat :** `BUILD SUCCESS`
- **Détails des tests :**
  - `ma.tourisme.avis.AvisSecurityIntegrationTest` : 14 tests exécutés, 0 échec, 0 erreur
  - `ma.tourisme.avis.client.ReservationClientTest` : 18 tests exécutés, 0 échec, 0 erreur
  - `ma.tourisme.avis.service.AvisServiceTest` : 33 tests exécutés, 0 échec, 0 erreur
  - **Total :** **65 tests exécutés, 0 échec, 0 erreur, 0 ignoré**
- **Artifact généré :** `avis-service-1.0.0.jar`

---

## 2. Frontend — Tests et Build

### a. Tests unitaires (Vitest / Angular)
- **Commande :**
  ```bash
  cd frontend ; npx ng test --watch=false
  ```
- **Résultat :** `PASS`
- **Détails :**
  - **14 fichiers de test passés sur 14**
  - **64 tests réussis sur 64**
  - Durée : ~16.19 s

### b. Build de production
- **Commande :**
  ```bash
  cd frontend ; npx ng build
  ```
- **Résultat :** `SUCCESS`
- **Détails :**
  - Bundle généré dans `frontend/dist/guide-touristique`
  - Total initial transféré : ~118 kB

---

## 3. Docker Compose — Build de l'image avis-service

- **Commande :**
  ```bash
  docker compose -f docker-compose.yml -f compose.auth.yml build avis-service
  ```
- **Résultat :** `SUCCESS`
- **Image produite :** `guide-touristique-main2-avis-service:latest`
- **Détails :** Compilation Maven multi-stage propre, export de l'image réussi.
