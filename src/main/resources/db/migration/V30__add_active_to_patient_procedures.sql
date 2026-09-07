-- V30: Adiciona soft delete à atribuição de procedimento ao paciente
-- Remover uma atribuição passa a ser inativação, nunca remoção física:
-- patient_procedures é pai de procedure_executions e de checkins, que carregam
-- histórico clínico (execuções realizadas e medições coletadas do paciente).

ALTER TABLE patient_procedures
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_patient_procedures_active ON patient_procedures(active);
