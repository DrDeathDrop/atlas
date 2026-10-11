--liquibase formatted sql

--changeset atlas:0013-create-simulations
CREATE TABLE simulation_runs (
    id               uuid             PRIMARY KEY,
    scenario         varchar(32)      NOT NULL,
    latitude         double precision NOT NULL,
    longitude        double precision NOT NULL,
    radius_km        double precision NOT NULL,
    planned          integer          NOT NULL CHECK (planned > 0),
    created          integer          NOT NULL CHECK (created >= 0),
    interval_seconds integer          NOT NULL CHECK (interval_seconds > 0),
    seed             bigint           NOT NULL,
    status           varchar(16)      NOT NULL,
    started_by       uuid             NOT NULL,
    started_at       timestamptz      NOT NULL,
    next_incident_at timestamptz      NOT NULL,
    finished_at      timestamptz,
    CONSTRAINT ck_simulation_runs_created_within_planned CHECK (created <= planned)
);

CREATE INDEX idx_simulation_runs_due ON simulation_runs (next_incident_at) WHERE status = 'RUNNING';
CREATE UNIQUE INDEX uq_simulation_runs_one_running ON simulation_runs (status) WHERE status = 'RUNNING';

CREATE TABLE simulation_incidents (
    run_id      uuid NOT NULL REFERENCES simulation_runs (id) ON DELETE CASCADE,
    incident_id uuid NOT NULL REFERENCES incidents (id) ON DELETE CASCADE,
    PRIMARY KEY (run_id, incident_id)
);

CREATE INDEX idx_simulation_incidents_incident ON simulation_incidents (incident_id);
