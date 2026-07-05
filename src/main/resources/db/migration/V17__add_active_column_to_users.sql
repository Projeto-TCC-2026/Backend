-- V17: Soft delete na tabela users
ALTER TABLE users ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;
