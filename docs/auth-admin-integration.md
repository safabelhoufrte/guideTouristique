# Authentification et administration

Cette branche contient uniquement la couche partagée d'authentification et l'interface Admin. Elle ne remplace pas les implémentations des services Guide, Réservation ou Avis.

## Lancement local

1. Copier `.env.example` vers `.env` et renseigner les mots de passe PostgreSQL métier.
2. Exécuter `python scripts/setup-local-auth.py` pour générer les secrets locaux de Keycloak.
3. Démarrer Keycloak avec `docker compose -f docker-compose.yml -f compose.auth.yml up -d`.
4. Démarrer le frontend puis ouvrir `/connexion`. La configuration publique est dans `frontend/public/auth-config.json`.

Keycloak est disponible localement sur `http://localhost:8180`. Les comptes de démonstration générés restent dans `.local/demo-accounts.txt`, qui est ignoré par Git.

## Contrat pour les services

Chaque service qui protège une API doit valider les JWT du realm `guide-touristique`, l'audience `guide-touristique-api`, le sujet et les rôles `USER`, `GUIDE`, `ADMIN`. Le frontend ne constitue jamais une preuve d'autorisation : le contrôle doit être fait dans le backend de chaque service.

## Périmètre

L'Admin permet la navigation vers les circuits, guides, réservations et avis. Les actions disponibles dépendent des API déjà fusionnées dans `main2`. Pour Circuit, fusionner d'abord la PR Circuit avant cette branche.
