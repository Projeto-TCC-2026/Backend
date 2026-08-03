-- Dados de teste para a aplicação TCC
-- Execute estes comandos no console H2: http://localhost:8081/h2-console

-- Inserir hospitais de teste
INSERT INTO hospitals (id, name, cnpj, phone, email, address, city, state, active, created_at, updated_at)
VALUES 
    (RANDOM_UUID(), 'Hospital São Lucas', '12345678000195', '(11) 98765-4321', 'contato@saolucas.com.br', 'Rua das Flores, 123', 'São Paulo', 'SP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (RANDOM_UUID(), 'Hospital Albert Einstein', '98765432000110', '(11) 87654-3210', 'contato@einstein.com.br', 'Av. Albert Einstein, 627', 'São Paulo', 'SP', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Inserir usuários de teste
-- Senha: "123456" (hash BCrypt)
INSERT INTO users (id, email, password_hash, role, active, created_at, updated_at, hospital_id)
VALUES 
    -- ADMIN (administrador geral)
    (RANDOM_UUID(), 'admin@tcc.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTUJYGsp6R9iO.WMfLq3qjfPjBF.Bqly', 'ADMIN', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null),
    
    -- DOCTOR (médico geral)
    (RANDOM_UUID(), 'doutor@tcc.com', '$2a$10$N.zmdr9k7uOCQb376NoUnuTUJYGsp6R9iO.WMfLq3qjfPjBF.Bqly', 'DOCTOR', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, null);

-- Inserir pacientes de teste (sem usuários associados)
INSERT INTO patients (id, user_id, full_name, cpf, birth_date, gender, blood_type, phone, email, address, city, state, zip_code, weight, height, active, created_at, updated_at)
VALUES 
    (RANDOM_UUID(), null, 'Maria da Silva Oliveira', '12345678901', '1985-05-15', 'FEMALE', 'A_POSITIVE', '(11) 98765-4321', 'maria.silva@email.com', 'Rua das Palmeiras, 456', 'São Paulo', 'SP', '01234567', 65.5, 1.65, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (RANDOM_UUID(), null, 'João Carlos Santos', '98765432109', '1990-12-10', 'MALE', 'O_POSITIVE', '(11) 87654-3210', 'joao.carlos@email.com', 'Av. Paulista, 1000', 'São Paulo', 'SP', '01310100', 80.0, 1.78, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (RANDOM_UUID(), null, 'Ana Paula Costa', '45678912345', '1992-08-22', 'FEMALE', 'B_POSITIVE', '(11) 76543-2109', 'ana.costa@email.com', 'Rua Augusta, 789', 'São Paulo', 'SP', '01305001', 58.3, 1.60, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (RANDOM_UUID(), null, 'Pedro Henrique Souza', '78912345678', '1988-03-18', 'MALE', 'AB_POSITIVE', '(11) 65432-1098', 'pedro.souza@email.com', 'Rua da Consolação, 321', 'São Paulo', 'SP', '01302001', 75.2, 1.72, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    (RANDOM_UUID(), null, 'Carla Fernanda Lima', '65432178909', '1995-11-07', 'FEMALE', 'O_NEGATIVE', '(11) 54321-0987', 'carla.lima@email.com', 'Av. Faria Lima, 654', 'São Paulo', 'SP', '04538132', 62.8, 1.58, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);