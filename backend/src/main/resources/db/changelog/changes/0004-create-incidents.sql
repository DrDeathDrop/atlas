--liquibase formatted sql

--changeset atlas:0004-create-incidents
CREATE SEQUENCE incident_reference_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE incidents (
    id              uuid                  PRIMARY KEY,
    reference       varchar(32)           NOT NULL UNIQUE,
    title           varchar(200)          NOT NULL,
    description     text                  NOT NULL,
    category        varchar(32)           NOT NULL,
    severity        varchar(16)           NOT NULL,
    status          varchar(16)           NOT NULL,
    location        geometry(Point, 4326) NOT NULL,
    affected_people integer               NOT NULL,
    reported_by     uuid                  NOT NULL REFERENCES users (id),
    created_at      timestamptz           NOT NULL,
    updated_at      timestamptz           NOT NULL
);

CREATE INDEX idx_incidents_status ON incidents (status);
CREATE INDEX idx_incidents_location ON incidents USING GIST (location);
