--liquibase formatted sql

--changeset atlas:0008-create-assignments
CREATE TABLE assignments (
    id          uuid        PRIMARY KEY,
    resource_id uuid        NOT NULL REFERENCES resources (id),
    incident_id uuid        NOT NULL REFERENCES incidents (id),
    assigned_by uuid        NOT NULL,
    assigned_at timestamptz NOT NULL,
    released_by uuid,
    released_at timestamptz
);

CREATE UNIQUE INDEX uq_assignments_open_resource ON assignments (resource_id) WHERE released_at IS NULL;
CREATE INDEX idx_assignments_incident ON assignments (incident_id);

--changeset atlas:0008-add-audit-related-entity
ALTER TABLE audit_log ADD COLUMN related_entity_id uuid;

CREATE INDEX idx_audit_log_related_entity ON audit_log (related_entity_id);
