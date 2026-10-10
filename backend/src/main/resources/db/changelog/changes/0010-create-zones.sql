--liquibase formatted sql

--changeset atlas:0010-create-zones
CREATE TABLE zones (
    id         uuid                    PRIMARY KEY,
    name       varchar(120)            NOT NULL,
    type       varchar(16)             NOT NULL,
    boundary   geometry(Polygon, 4326) NOT NULL,
    created_by uuid                    NOT NULL,
    created_at timestamptz             NOT NULL,
    lifted_by  uuid,
    lifted_at  timestamptz
);

CREATE INDEX idx_zones_boundary ON zones USING GIST (boundary);
CREATE INDEX idx_zones_active ON zones (created_at) WHERE lifted_at IS NULL;

--changeset atlas:0010-create-road-closures
CREATE TABLE road_closures (
    id          uuid                       PRIMARY KEY,
    road_name   varchar(120)               NOT NULL,
    reason      varchar(255),
    path        geometry(LineString, 4326) NOT NULL,
    created_by  uuid                       NOT NULL,
    created_at  timestamptz                NOT NULL,
    reopened_by uuid,
    reopened_at timestamptz
);

CREATE INDEX idx_road_closures_path ON road_closures USING GIST (path);
CREATE INDEX idx_road_closures_active ON road_closures (created_at) WHERE reopened_at IS NULL;
