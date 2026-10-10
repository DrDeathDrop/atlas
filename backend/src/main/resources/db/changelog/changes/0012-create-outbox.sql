--liquibase formatted sql

--changeset atlas:0012-create-outbox
CREATE TABLE outbox_events (
    id           uuid        PRIMARY KEY,
    routing_key  varchar(64) NOT NULL,
    payload      text        NOT NULL,
    created_at   timestamptz NOT NULL,
    published_at timestamptz
);

CREATE INDEX idx_outbox_unpublished ON outbox_events (created_at) WHERE published_at IS NULL;

--changeset atlas:0012-add-notification-event-id
ALTER TABLE notifications ADD COLUMN event_id uuid;

CREATE UNIQUE INDEX uq_notifications_event_recipient ON notifications (event_id, recipient_id)
    WHERE event_id IS NOT NULL;
