--liquibase formatted sql

--changeset atlas:0002-create-users
CREATE TABLE users (
                       id            uuid         PRIMARY KEY,
                       email         varchar(255) NOT NULL UNIQUE,
                       password_hash varchar(255) NOT NULL,
                       full_name     varchar(255) NOT NULL,
                       role          varchar(32)  NOT NULL,
                       enabled       boolean      NOT NULL,
                       created_at    timestamptz  NOT NULL,
                       updated_at    timestamptz  NOT NULL
);