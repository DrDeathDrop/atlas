--liquibase formatted sql

--changeset atlas:0006-create-application-user
CREATE ROLE atlas_app LOGIN;

GRANT USAGE ON SCHEMA public TO atlas_app;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO atlas_app;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO atlas_app;

ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO atlas_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT ON SEQUENCES TO atlas_app;

REVOKE UPDATE, DELETE, TRUNCATE ON audit_log FROM atlas_app;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON databasechangelog, databasechangeloglock FROM atlas_app;

--changeset atlas:0006-set-application-user-password runOnChange:true
ALTER ROLE atlas_app PASSWORD '${appUserPassword}';
