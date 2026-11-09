# Valider le parcours administrateur Circuit

Le formulaire Angular appelle `/api/circuits` avec le token Keycloak ajouté par
l'intercepteur. Circuit exige ADMIN et vérifie le guide auprès de Guide API
avant l'écriture. La création devient visible dans le catalogue public.
L'archivage masque le circuit sans effacer sa ligne SQL.

## Démarrer

Utiliser `auth-integration` pour les espaces privés et le vrai service Guide.
La branche `circuit-service` contient le backend de la PR vers `main2` ; son
frontend public ne contient pas l'espace admin.

Avec Docker Desktop démarré, depuis `auth-integration` :

```powershell
Copy-Item .env.example .env
# Renseigner les cinq mots de passe PostgreSQL dans .env.
python scripts/setup-local-auth.py
docker compose up --build -d postgres keycloak-db keycloak guide-service circuit-service frontend
```

Ne pas écraser un `.env` existant. Les identifiants locaux sont dans
`.local/demo-accounts.txt` : garder ce fichier, `.env` et les tokens hors de Git.
Pour un volume existant, suivre d'abord [la transition des données](shared-database.md),
sans supprimer les volumes. Réservation et Avis ne sont pas nécessaires pour ce parcours.

| Accès | Adresse locale |
|---|---|
| Gestion Circuit | http://localhost:4200/administration/circuits |
| Catalogue public | http://localhost:4200/circuits |
| Keycloak | http://localhost:8180 |
| Guide API | http://localhost:8081/api/guides |
| Circuit API | http://localhost:8082/api/circuits |
| Base métier | localhost:15433, tourisme_db, schémas guide et circuit |

## Démontrer dans le navigateur

1. Se connecter avec `demo-admin`. Créer un guide depuis l'espace admin si la
   base est vide, puis ouvrir **Circuits → Créer un circuit**.
2. Renseigner titre, description, ville, durée entière positive, prix et guide.
   Le prix zéro est autorisé. Le bouton est désactivé pendant l'enregistrement.
3. Vérifier la liste admin, `/circuits` et la fiche publique. Modifier le titre
   et le prix ; vérifier que la même fiche conserve son ID.
4. Archiver après confirmation. Le circuit disparaît du catalogue et sa fiche
   publique renvoie 404 ; sa ligne reste conservée dans PostgreSQL.
5. Effacer durée ou prix : la validation bloque l'envoi. Une erreur API doit
   conserver les saisies pour permettre une nouvelle tentative.
6. Avec `demo-touriste`, l'espace admin est refusé. Une écriture API reçoit 403
   avec ce compte et 401 sans token.

Sur une installation de démonstration dédiée, ouvrir et remplir le formulaire,
puis arrêter Guide avec `docker compose stop guide-service`. Enregistrer : le
message d'indisponibilité doit apparaître sans perte des champs ni création.
Relancer avec `docker compose start guide-service` puis réessayer. Ne pas
arrêter Guide sur une installation utilisée par l'équipe.

## Vérification reproductible avec les vrais services

```powershell
python scripts/verify-circuit-admin.py
```

Ce script utilise les comptes de démonstration existants et le vrai flux
authorization code avec PKCE, sans activer le password grant. Il teste les
refus 400/401/403, la création avec le vrai Guide, la lecture publique, la
modification et l'archivage. Il crée ses propres données puis les archive ;
aucun enregistrement préexistant n'est modifié. Tokens et mots de passe ne sont
pas affichés. Pour un environnement dédié : `--env-file chemin/vers/test.env`.
Ce contrôle HTTP ne remplace pas la démonstration visuelle du navigateur.

## Tests automatiques

Dans `frontend` : `npm ci`, `npm test -- --watch=false`, `npm run build`.
`circuit-editor.spec.ts` vérifie prix zéro, champs numériques obligatoires,
double envoi, conservation des champs après échec et modification du même ID.
Les tests d'authentification vérifient rôles et ajout du Bearer token.

Dans la branche `circuit-service`, depuis le module : `mvn -B -ntp verify`
avec Docker. Les 46 tests comprennent 16 tests métier isolés, 14 tests du client
HTTP Guide et 16 tests d'intégration Spring/PostgreSQL/JWT. Les rapports
Surefire sont conservés en artefact par la CI.

## Validation effectuée

Une installation Docker dédiée a utilisé le backend `circuit-service` au commit
`a286498`, Guide et Keycloak de `auth-integration`, avec le frontend admin corrigé.
Les 46 tests backend et les 39 tests frontend ont réussi ; le build Docker du
frontend a réussi. Le script ci-dessus a validé les vrais échanges HTTP et JWT.

Dans le navigateur, la connexion ADMIN, la création d'un guide et d'un circuit,
la consultation du catalogue et de la fiche publique, la modification du prix
et l'archivage ont été vérifiés. L'arrêt du vrai Guide pendant l'enregistrement
a affiché le message d'indisponibilité tout en conservant les champs ; après
redémarrage, la même saisie a été enregistrée avec succès. La ligne du circuit
archivé est conservée en SQL. Ces résultats concernent cette installation locale,
pas une fusion dans `main2` ni un déploiement partagé.
