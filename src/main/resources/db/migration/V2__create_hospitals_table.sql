-- V2: Tabela de hospitais

CREATE TABLE hospitals (
    id         UUID         NOT NULL PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    cnpj       VARCHAR(14)  NOT NULL UNIQUE,
    phone      VARCHAR(20),
    email      VARCHAR(255),
    address    VARCHAR(255),
    city       VARCHAR(100),
    state      VARCHAR(2),
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_hospitals_active ON hospitals(active);
