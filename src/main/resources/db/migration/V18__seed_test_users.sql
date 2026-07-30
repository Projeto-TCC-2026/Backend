-- V18: Seed de usuários de teste para login local
-- Credenciais:
--   admin@tcc.com / 123456
--   doctor@tcc.com / 123456
--   hospital@tcc.com / 123456
--   patient@tcc.com / 123456

-- 1) Hospital base para o médico e o gestor
INSERT INTO hospitals (id, name, cnpj, phone, email, address, city, state, active, created_at, updated_at)
SELECT gen_random_uuid(), 'Hospital São Lucas', '12345678000199', '1133334444', 'contato@saolucas.com', 'Rua das Flores, 100', 'São Paulo', 'SP', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM hospitals WHERE cnpj = '12345678000199');

-- 2) Usuários
INSERT INTO users (id, email, password_hash, role, active, created_at, updated_at, hospital_id)
SELECT gen_random_uuid(), 'admin@tcc.com', '$2b$12$wAFqcmbmvmSIPTj/pXyDNukw/eA.vbQTgk18Zk8rRFCtOfWj.Eopq', 'ADMIN', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'admin@tcc.com');

INSERT INTO users (id, email, password_hash, role, active, created_at, updated_at, hospital_id)
SELECT gen_random_uuid(), 'doctor@tcc.com', '$2b$12$wAFqcmbmvmSIPTj/pXyDNukw/eA.vbQTgk18Zk8rRFCtOfWj.Eopq', 'DOCTOR', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'doctor@tcc.com');

INSERT INTO users (id, email, password_hash, role, active, created_at, updated_at, hospital_id)
SELECT gen_random_uuid(), 'hospital@tcc.com', '$2b$12$wAFqcmbmvmSIPTj/pXyDNukw/eA.vbQTgk18Zk8rRFCtOfWj.Eopq', 'HOSPITAL', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP,
       (SELECT id FROM hospitals WHERE cnpj = '12345678000199')
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'hospital@tcc.com');

INSERT INTO users (id, email, password_hash, role, active, created_at, updated_at, hospital_id)
SELECT gen_random_uuid(), 'patient@tcc.com', '$2b$12$wAFqcmbmvmSIPTj/pXyDNukw/eA.vbQTgk18Zk8rRFCtOfWj.Eopq', 'PATIENT', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, NULL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'patient@tcc.com');

-- 3) Perfil de médico
INSERT INTO doctors (id, user_id, hospital_id, full_name, cpf, crm, specialty, phone, created_at, updated_at)
SELECT gen_random_uuid(), u.id, h.id, 'Dr. Carlos Mendes', '12345678901', 'CRM-SP-10001', 'Cardiologia', '11988887777', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
JOIN hospitals h ON h.cnpj = '12345678000199'
WHERE u.email = 'doctor@tcc.com'
  AND NOT EXISTS (SELECT 1 FROM doctors WHERE cpf = '12345678901');

-- 4) Perfil de paciente
INSERT INTO patients (id, user_id, full_name, cpf, birth_date, gender, phone, email, address, city, state, zip_code, blood_type, weight, height, active, created_at, updated_at)
SELECT gen_random_uuid(), u.id, 'Ana Paula Silva', '11122233344', '1990-05-15', 'FEMININO', '11977776666', 'patient@tcc.com', 'Rua das Acácias, 200', 'Campinas', 'SP', '13000-000', 'O+', 62.5, 1.68, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM users u
WHERE u.email = 'patient@tcc.com'
  AND NOT EXISTS (SELECT 1 FROM patients WHERE cpf = '11122233344');
