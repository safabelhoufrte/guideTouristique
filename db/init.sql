\set ON_ERROR_STOP on
\getenv db_name POSTGRES_DB
\getenv guide_password GUIDE_DB_PASSWORD
\getenv circuit_password CIRCUIT_DB_PASSWORD
\getenv reservation_password RESERVATION_DB_PASSWORD
\getenv avis_password AVIS_DB_PASSWORD

-- First-time provisioning only. Each service owns its schema and migrations.
-- psql quotes the password variables as SQL literals; no shell substitution.
BEGIN;

CREATE ROLE guide LOGIN PASSWORD :'guide_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
CREATE ROLE circuit LOGIN PASSWORD :'circuit_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
CREATE ROLE reservation LOGIN PASSWORD :'reservation_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;
CREATE ROLE avis LOGIN PASSWORD :'avis_password'
    NOSUPERUSER NOCREATEDB NOCREATEROLE NOINHERIT;

REVOKE ALL ON DATABASE :"db_name" FROM PUBLIC;
GRANT CONNECT ON DATABASE :"db_name" TO guide, circuit, reservation, avis;
REVOKE ALL ON SCHEMA public FROM PUBLIC;

CREATE SCHEMA guide AUTHORIZATION guide;
CREATE SCHEMA circuit AUTHORIZATION circuit;
CREATE SCHEMA reservation AUTHORIZATION reservation;
CREATE SCHEMA avis AUTHORIZATION avis;

ALTER ROLE guide IN DATABASE :"db_name" SET search_path = guide;
ALTER ROLE circuit IN DATABASE :"db_name" SET search_path = circuit;
ALTER ROLE reservation IN DATABASE :"db_name" SET search_path = reservation;
ALTER ROLE avis IN DATABASE :"db_name" SET search_path = avis;

-- No business tables or cross-schema grants: service migrations create tables.
COMMIT;
