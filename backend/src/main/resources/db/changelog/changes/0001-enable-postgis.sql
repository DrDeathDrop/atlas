--liquibase formatted sql

--changeset atlas:0001-enable-postgis
CREATE EXTENSION IF NOT EXISTS postgis;
