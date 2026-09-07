-- V29: Adiciona soft delete ao vínculo doutor-procedimento
-- Desatrelar um médico de um procedimento passa a ser inativação do vínculo,
-- nunca remoção física: doctor_procedures é pai de doctor_procedure_fields, que por
-- sua vez é referenciado por checkin_field_values (medições já coletadas).

ALTER TABLE doctor_procedures
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_doctor_procedures_active ON doctor_procedures(active);
