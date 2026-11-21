CREATE TABLE guide_photo (
    guide_id BIGINT PRIMARY KEY
        REFERENCES guide(id) ON DELETE CASCADE,
    content_type VARCHAR(20) NOT NULL
        CHECK (content_type IN ('image/jpeg','image/png','image/webp')),
    data BYTEA NOT NULL
);
