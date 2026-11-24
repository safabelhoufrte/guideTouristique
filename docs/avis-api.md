\# API du service Avis



Service : `avis-service`, port 8084. Base : `/api/avis`.



\## Routes



| Méthode | Route | Accès | Description |

|---|---|---|---|

| GET | `/api/avis` | public | Liste des avis publiés (filtres `circuitId`, `guideId`) |

| GET | `/api/avis/moyenne` | public | Note moyenne d'un circuit ou d'un guide |

| GET | `/api/avis/{id}` | public | Détail d'un avis |

| GET | `/api/avis/eligibilite` | USER | Réservations éligibles du voyageur connecté |

| GET | `/api/avis/me` | USER | Avis du voyageur connecté |

| POST | `/api/avis` | USER | Publier un avis |

| PATCH | `/api/avis/{id}/publication` | ADMIN | Publier ou masquer un avis |

| DELETE | `/api/avis/{id}` | ADMIN | Supprimer un avis |



\## Règle d'éligibilité



Un avis n'est possible que si la réservation :

\- appartient à l'utilisateur connecté (email du token) ;

\- a le statut `ACCEPTEE` ;

\- a une date de visite passée.



L'identité vient toujours du token, jamais du corps de la requête.



\## Codes de réponse de `POST /api/avis`



| Code | Cas |

|---|---|

| 201 | Avis créé |

| 400 | Note hors de 1 à 5 ou commentaire invalide |

| 401 | Pas de token |

| 403 | Réservation d'un autre utilisateur, non acceptée ou future |

| 409 | Un avis existe déjà pour cette réservation |

| 503 | `reservation-service` indisponible ou réponse invalide |



\## Tests



\- `mvn -B -ntp verify` (65 tests)

\- `mvn -B -ntp checkstyle:check` (0 violation)

\- Collection Postman : `docs/livrables/avis-service.postman\_collection.json`, exécutable avec Newman.

