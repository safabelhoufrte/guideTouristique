# Tests Postman de l'API Circuit

`circuit-api.postman_collection.json` : 29 requêtes, 56 assertions, en 4 dossiers.

| Dossier | Ce qui est vérifié |
| --- | --- |
| 1. Lecture publique | Liste, filtre par ville, détail, 404, 400, photo (type, ETag SHA-256, 304 en cache) |
| 2. Sécurité | 401 sans jeton ou avec un jeton invalide, 403 pour un touriste |
| 3. Administrateur | Validation (400), guide inconnu, création, lecture, modification, archivage |
| 4. Guide | Ses propres circuits seulement : 403 sur le circuit ou le guideId d'un autre guide |

Les circuits créés par la collection sont archivés à la fin.

## Lancer

Dans Postman : importer la collection et `local.postman_environment.json`, puis renseigner
`adminToken`, `guideToken` et `touristToken` (jetons d'accès Keycloak des comptes de démo).
Les jetons ne sont jamais enregistrés dans le dépôt.

En ligne de commande (Newman) :

```bash
npx newman run circuit-api.postman_collection.json -e local.postman_environment.json \
  --env-var adminToken=... --env-var guideToken=... --env-var touristToken=...
```

`baseUrl` vaut `http://localhost:8082` (service Circuit) et `guideApiUrl` `http://localhost:8081`
(service Guide, pour `/api/guides/me`).
