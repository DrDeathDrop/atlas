--liquibase formatted sql

--changeset atlas:0007-create-resources
CREATE TABLE resources (
    id         uuid                  PRIMARY KEY,
    call_sign  varchar(64)           NOT NULL UNIQUE,
    type       varchar(32)           NOT NULL,
    status     varchar(16)           NOT NULL,
    location   geometry(Point, 4326) NOT NULL,
    team_id    uuid                  REFERENCES resources (id),
    version    bigint                NOT NULL,
    created_at timestamptz           NOT NULL,
    updated_at timestamptz           NOT NULL
);

CREATE INDEX idx_resources_status ON resources (status);
CREATE INDEX idx_resources_location ON resources USING GIST ((location::geography));
