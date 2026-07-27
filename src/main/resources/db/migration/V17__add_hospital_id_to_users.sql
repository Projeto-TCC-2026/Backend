-- V17: Associa um usuário gestor a um hospital
-- A coluna é NULL para roles ADMIN, DOCTOR e PATIENT.
-- O UNIQUE garante que cada hospital tenha no máximo um usuário com role HOSPITAL.

ALTER TABLE users
    ADD COLUMN hospital_id UUID,
    ADD CONSTRAINT fk_users_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id),
    ADD CONSTRAINT uq_users_hospital_id UNIQUE (hospital_id);
