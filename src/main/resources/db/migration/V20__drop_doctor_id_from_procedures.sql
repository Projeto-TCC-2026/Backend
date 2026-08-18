-- V21: Remove o dono único do procedimento
-- O vínculo com médicos vive em doctor_procedures desde a V20.
-- Depende de: V19, V20

ALTER TABLE procedures DROP CONSTRAINT fk_procedures_doctor;

ALTER TABLE procedures DROP COLUMN doctor_id;
