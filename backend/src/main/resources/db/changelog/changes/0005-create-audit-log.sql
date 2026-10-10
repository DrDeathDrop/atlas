--liquibase formatted sql

--changeset atlas:0005-create-audit-log
CREATE TABLE audit_log (
    id               uuid        PRIMARY KEY,
    occurred_at      timestamptz NOT NULL,
    actor_id         uuid        NOT NULL,
    actor_role       varchar(32) NOT NULL,
    action           varchar(64) NOT NULL,
    entity_type      varchar(64) NOT NULL,
    entity_id        uuid        NOT NULL,
    entity_reference varchar(64),
    previous_state   varchar(64),
    new_state        varchar(64)
);

CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
CREATE INDEX idx_audit_log_occurred_at ON audit_log (occurred_at);
