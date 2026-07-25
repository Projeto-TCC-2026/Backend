-- V15: Tabela de refresh tokens
-- Depende de: users

CREATE TABLE refresh_tokens (
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    expires_at TIMESTAMP    NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);
