CREATE TABLE IF NOT EXISTS account_activation_tokens (
    id UUID PRIMARY KEY,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_account_activation_user FOREIGN KEY (user_id) REFERENCES users(id)
);
