CREATE TABLE guide (
    id             BIGSERIAL PRIMARY KEY,
    nom            VARCHAR(100) NOT NULL,
    prenom         VARCHAR(100),
    ville          VARCHAR(100),
    email          VARCHAR(150),
    telephone      VARCHAR(30),
    tarif_par_jour DOUBLE PRECISION NOT NULL DEFAULT 0,
    actif          BOOLEAN NOT NULL DEFAULT TRUE,
    note_moyenne   DOUBLE PRECISION
);

CREATE TABLE guide_langues (
    guide_id BIGINT NOT NULL REFERENCES guide(id) ON DELETE CASCADE,
    langue   VARCHAR(50) NOT NULL,
    PRIMARY KEY (guide_id, langue)
);