-- V32: Tabela de tokens de push do aplicativo mobile
-- Depende de: users

CREATE TABLE device_tokens (
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL,
    token      VARCHAR(512) NOT NULL UNIQUE,
    platform   VARCHAR(20)  NOT NULL,
    device_id  VARCHAR(255),
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_device_tokens_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_device_tokens_user_id ON device_tokens (user_id);
