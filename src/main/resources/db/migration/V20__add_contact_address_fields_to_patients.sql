-- V20: Adiciona campos de contato e endereço na tabela patients
-- Para melhorar buscas e filtros

ALTER TABLE patients ADD COLUMN email VARCHAR(255);
ALTER TABLE patients ADD COLUMN address VARCHAR(500);
ALTER TABLE patients ADD COLUMN city VARCHAR(100);
ALTER TABLE patients ADD COLUMN state VARCHAR(2);
ALTER TABLE patients ADD COLUMN zip_code VARCHAR(10);
ALTER TABLE patients ADD COLUMN blood_type VARCHAR(10);

-- Índices para otimizar buscas
CREATE INDEX idx_patients_email ON patients(email);
CREATE INDEX idx_patients_phone ON patients(phone);
CREATE INDEX idx_patients_city ON patients(city);
CREATE INDEX idx_patients_state ON patients(state);
CREATE INDEX idx_patients_gender ON patients(gender);
CREATE INDEX idx_patients_full_name ON patients(full_name);
CREATE INDEX idx_patients_created_at ON patients(created_at);
