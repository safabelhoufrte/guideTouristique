-- Additive migration: legacy records retain null for previously unknown fields.
-- New requests require all three fields through CircuitRequest validation.
ALTER TABLE circuit.circuits ADD COLUMN description VARCHAR(4000);
ALTER TABLE circuit.circuits ADD COLUMN ville VARCHAR(120);
ALTER TABLE circuit.circuits ADD COLUMN duree_heures INTEGER CHECK (duree_heures > 0);
