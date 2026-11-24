CREATE TABLE codes_promo (
    code VARCHAR(40) PRIMARY KEY,
    pourcentage INTEGER NOT NULL CHECK (pourcentage BETWEEN 1 AND 100),
    actif BOOLEAN NOT NULL DEFAULT TRUE,
    date_expiration DATE,
    utilisations_max INTEGER,
    utilisations INTEGER NOT NULL DEFAULT 0,
    personnes_min INTEGER NOT NULL DEFAULT 1,
    premiere_reservation_seulement BOOLEAN NOT NULL DEFAULT FALSE
);

ALTER TABLE reservations ADD COLUMN code_promo VARCHAR(40);
ALTER TABLE reservations ADD COLUMN reduction_pct INTEGER NOT NULL DEFAULT 0;

INSERT INTO codes_promo (code, pourcentage, personnes_min, premiere_reservation_seulement) VALUES
    ('BIENVENUE', 15, 1, TRUE),
    ('PROMO10', 10, 1, FALSE),
    ('GROUPE20', 20, 4, FALSE);