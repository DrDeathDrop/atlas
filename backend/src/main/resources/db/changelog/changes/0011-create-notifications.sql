--liquibase formatted sql

--changeset atlas:0011-create-notifications
CREATE TABLE notifications (
    id           uuid         PRIMARY KEY,
    recipient_id uuid         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type         varchar(32)  NOT NULL,
    title        varchar(200) NOT NULL,
    incident_id  uuid         REFERENCES incidents (id) ON DELETE CASCADE,
    created_at   timestamptz  NOT NULL,
    read_at      timestamptz
);

CREATE INDEX idx_notifications_recipient ON notifications (recipient_id, created_at DESC);
CREATE INDEX idx_notifications_unread ON notifications (recipient_id) WHERE read_at IS NULL;
