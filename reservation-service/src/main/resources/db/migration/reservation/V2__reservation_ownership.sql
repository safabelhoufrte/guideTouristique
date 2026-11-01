-- Legacy reservations remain administrator-only; never guess ownership from an email.
ALTER TABLE reservations ADD COLUMN user_subject VARCHAR(100);
ALTER TABLE reservations ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_reservations_user ON reservations(user_subject);
CREATE INDEX idx_reservations_guide ON reservations(guide_id);
