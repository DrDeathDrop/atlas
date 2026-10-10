--liquibase formatted sql

--changeset atlas:0009-create-facilities
CREATE TABLE facilities (
    id         uuid                  PRIMARY KEY,
    name       varchar(120)          NOT NULL UNIQUE,
    type       varchar(16)           NOT NULL,
    address    varchar(255),
    location   geometry(Point, 4326) NOT NULL,
    capacity   integer               NOT NULL CHECK (capacity >= 0),
    occupancy  integer               NOT NULL CHECK (occupancy >= 0),
    created_at timestamptz           NOT NULL,
    updated_at timestamptz           NOT NULL,
    CONSTRAINT ck_facilities_occupancy_within_capacity CHECK (occupancy <= capacity)
);

CREATE INDEX idx_facilities_location ON facilities USING GIST ((location::geography));
