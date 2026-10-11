--liquibase formatted sql

--changeset atlas:0014-add-simulation-related-incidents
ALTER TABLE simulation_runs ADD COLUMN related_incidents boolean NOT NULL DEFAULT false;
