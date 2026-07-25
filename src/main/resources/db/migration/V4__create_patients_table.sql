-- V4: Tabela de pacientes
-- Depende de: users

CREATE TABLE patients (
    id         UUID         NOT NULL PRIMARY KEY,
    user_id    UUID         NOT NULL UNIQUE,
    full_name  VARCHAR(255) NOT NULL,
    cpf        VARCHAR(11)  NOT NULL UNIQUE,
    birth_date DATE         NOT NULL,
    gender     VARCHAR(20),
    phone      VARCHAR(20),
    email      VARCHAR(255),
    address    VARCHAR(500),
    city       VARCHAR(100),
    state      VARCHAR(2),
    zip_code   VARCHAR(10),
    blood_type VARCHAR(10),
    weight     DOUBLE PRECISION,
    height     DOUBLE PRECISION,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL,
    updated_at TIMESTAMP    NOT NULL,

    CONSTRAINT fk_patients_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_patients_active ON patients(active);
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_phone ON patients(phone);
CREATE INDEX idx_patients_city ON patients(city);
CREATE INDEX idx_patients_state ON patients(state);
CREATE INDEX idx_patients_gender ON patients(gender);
CREATE INDEX idx_patients_full_name ON patients(full_name);
CREATE INDEX idx_patients_created_at ON patients(created_at);
