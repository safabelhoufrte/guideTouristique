# Test de charge JMeter de l'application

`guide-touristique-load.jmx` teste toute l'application à travers nginx (`http://localhost:4200`) :
le frontend et les quatre services (Guide, Circuit, Réservation, Avis).

| Groupe | Parcours | Propriété (défaut) |
| --- | --- | --- |
| Visiteur anonyme | Accueil (HTML, JS, CSS), circuits, détail, photo, guide, photo du guide, avis, note | `visitors` (40) |
| Touriste connecté | Circuits, mes réservations, offre de bienvenue, mes avis | `tourists` (10) |
| Guide connecté | Mon profil, mes circuits, réservations reçues | `guides` (5) |
| Administrateur | Tous les guides, tous les avis, circuits | `admins` (5) |

Autres propriétés : `ramp` (30 s), `duration` (150 s), `host`, `port`, `protocol`.
Le test ne fait que des lectures : il ne remplit pas la base.

## Jetons

Les profils connectés lisent leur jeton Keycloak dans des propriétés JMeter. Créez un fichier
`tokens.properties` hors du dépôt, juste avant l'essai (un jeton vit 5 minutes) :

```properties
adminToken=...
guideToken=...
touristToken=...
touristEmail=...
```

Ne versionnez jamais ce fichier et supprimez-le après l'essai.

## Lancer

```bash
jmeter -n -t guide-touristique-load.jmx -q tokens.properties \
  -Jvisitors=40 -Jtourists=10 -Jguides=5 -Jadmins=5 \
  -l results.jtl -e -o dashboard \
  -Jjmeter.reportgenerator.apdex_satisfied_threshold=500 \
  -Jjmeter.reportgenerator.apdex_tolerated_threshold=1500
```

Le tableau de bord HTML est dans `dashboard/index.html`. Sa ligne « Total » ne calcule les
centiles que sur les 20 000 dernières requêtes (`jmeter.reportgenerator.statistic_window`) :
pour un long essai, recalculez-les depuis `results.jtl`.
