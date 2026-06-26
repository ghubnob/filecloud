CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE resources (
    id BIGSERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    path TEXT NOT NULL,
    name TEXT NOT NULL,
    size BIGINT NOT NULL DEFAULT 0,
    resource_type VARCHAR(20) NOT NULL
);

CREATE INDEX idx_resources_user_id_name ON resources(user_id, name);

CREATE INDEX idx_resources_name_trgm
    ON resources
    USING gin(lower(name) gin_trgm_ops);

ALTER TABLE resources ADD CONSTRAINT uk_resource_unique UNIQUE(user_id, path, name);

ALTER TABLE resources ADD CONSTRAINT chk_resource_type CHECK(resource_type IN ('FILE','DIRECTORY'));