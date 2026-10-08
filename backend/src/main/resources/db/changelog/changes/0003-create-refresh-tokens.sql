--liquibase formatted sql

--changeset atlas:0003-create-refresh-tokens
CREATE TABLE refresh_tokens (
    id         uuid         PRIMARY KEY,
    user_id    uuid         NOT NULL REFERENCES users (id),
    token_hash varchar(64)  NOT NULL UNIQUE,
    created_at timestamptz  NOT NULL,
    expires_at timestamptz  NOT NULL,
    revoked_at timestamptz,
    user_agent varchar(255),
    ip_address varchar(45)
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
