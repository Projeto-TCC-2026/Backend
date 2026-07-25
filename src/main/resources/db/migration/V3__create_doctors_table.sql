-- V3: Tabela de doutores
-- Depende de: users, hospitals

CREATE TABLE doctors (
    id          UUID         NOT NULL PRIMARY KEY,
    user_id     UUID         NOT NULL UNIQUE,
    hospital_id UUID         NOT NULL,
    full_name   VARCHAR(255) NOT NULL,
    cpf         VARCHAR(11)  NOT NULL UNIQUE,
    crm         VARCHAR(20)  NOT NULL UNIQUE,
    specialty   VARCHAR(100),
    phone       VARCHAR(20),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,

    CONSTRAINT fk_doctors_user     FOREIGN KEY (user_id)     REFERENCES users(id),
    CONSTRAINT fk_doctors_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id)
);
