-- V18: Usuário admin para desenvolvimento/testes
-- senha: password (BCrypt)
INSERT INTO users (email, password_hash, role, active, created_at, updated_at)
VALUES ('admin@tcc.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lHuu', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
