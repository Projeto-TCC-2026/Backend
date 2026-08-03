-- V19: Procedimento passa a pertencer ao hospital, não ao médico
-- O catálogo de procedimentos é definido pelo hospital; o vínculo com médicos
-- passa a ser N:N na tabela doctor_procedures (V20).
-- Depende de: procedures, doctors, hospitals

ALTER TABLE procedures ADD COLUMN hospital_id UUID;

-- Backfill: o hospital do procedimento é o hospital do médico dono atual
UPDATE procedures p
SET hospital_id = (SELECT d.hospital_id FROM doctors d WHERE d.id = p.doctor_id)
WHERE p.hospital_id IS NULL;

ALTER TABLE procedures ALTER COLUMN hospital_id SET NOT NULL;

ALTER TABLE procedures ADD CONSTRAINT fk_procedures_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
