-- V31: E-mail do paciente passa a ser obrigatório.
-- Regulariza os pacientes existentes sem e-mail copiando o e-mail do usuário
-- vinculado (users.email é NOT NULL, então nenhuma linha fica nula) e só depois
-- aplica a restrição NOT NULL.

UPDATE patients
SET email = (SELECT u.email FROM users u WHERE u.id = patients.user_id)
WHERE email IS NULL;

ALTER TABLE patients
    ALTER COLUMN email SET NOT NULL;
