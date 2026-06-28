-- Adiciona coluna 'active' na tabela patients
ALTER TABLE patients 
ADD COLUMN active BOOLEAN NOT NULL DEFAULT true;

-- Cria índice para otimizar consultas por status ativo
CREATE INDEX idx_patients_active ON patients(active);