# Base métier partagée

Les quatre services utilisent la même base PostgreSQL `tourisme_db`.
Les schémas `guide`, `circuit`, `reservation` et `avis` regroupent leurs tables
et historiques Flyway respectifs dans cette base. Chaque compte conserve
uniquement les permissions de son schéma. Les échanges métier restent en REST.
La base technique de Keycloak est indépendante.

## Installation neuve

`docker compose up --build` initialise la base via `db/init.sql` sur un volume
neuf. Les migrations Flyway existantes créent ensuite les tables. Les connexions
Spring utilisent le schéma du service, y compris les requêtes JDBC et Hibernate
sans préfixe de schéma.

## Volume contenant les quatre anciennes bases

Les scripts de `docker-entrypoint-initdb.d` ne sont exécutés qu'au premier
démarrage d'un volume neuf. Cette modification de configuration ne transfère
pas les données et ne supprime aucune ancienne base.

Avant de redémarrer les services sur un volume existant :

1. Arrêter les quatre services pour suspendre les écritures. Sauvegarder les
   bases `guide_db`, `circuit_db`, `reservation_db` et `avis_db` avec
   `pg_dump -Fc`, et les rôles avec `pg_dumpall --roles-only`. Conserver aussi
   la configuration et les images précédentes pour le retour arrière.
2. En administrateur, créer `tourisme_db` et lui appliquer les permissions,
   schémas et `search_path` de `db/init.sql`. Les quatre rôles existent déjà :
   ne pas réexécuter les instructions `CREATE ROLE`.
3. Restaurer les sauvegardes dans des bases temporaires. Dans celles de Guide,
   Réservation et Avis, renommer le schéma `public` en schéma du service.
   Circuit possède déjà son schéma `circuit`. Exporter ensuite chaque schéma
   avec `pg_dump -Fc --schema=<service>`.
4. Restaurer ces exports dans `tourisme_db` avec `pg_restore --exit-on-error`,
   en conservant les propriétaires. Les schémas vides précréés doivent être
   retirés avant la restauration ; ne jamais employer `CASCADE` ni supprimer
   un schéma contenant déjà des données. Restaurer aussi les historiques
   Flyway, séquences, contraintes et index, sans réappliquer V1 manuellement.
   Après restauration, exécuter `ALTER SCHEMA <service> OWNER TO <service>`
   et `REVOKE ALL ON SCHEMA <service> FROM PUBLIC` pour chaque schéma ; vérifier
   les propriétaires des tables et séquences.
5. Comparer les lignes, identifiants, séquences, contraintes et historiques
   Flyway avec les sources. Démarrer les services avec les nouvelles URLs,
   puis vérifier les parcours Guide, Circuit, Réservation et Avis.

Conserver les anciennes bases et sauvegardes jusqu'à validation. Avant toute
nouvelle écriture dans la base partagée, le retour arrière consiste à rétablir
la configuration et les images précédentes puis redémarrer sur les anciennes
bases. Après de nouvelles écritures, un retour arrière exige d'abord leur
réconciliation ; ne pas revenir simplement à une copie devenue obsolète.
Ne pas utiliser `docker compose down -v` pour cette transition.
