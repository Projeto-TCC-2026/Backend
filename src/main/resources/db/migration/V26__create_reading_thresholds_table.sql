-- V26: Faixas normais de leitura de sinais vitais
-- Cada linha define o intervalo NORMAL de um tipo de leitura. O alerta é gerado
-- quando o valor medido sai dessa faixa. Limite nulo = sem limite desse lado.
-- Substitui os limites que hoje vivem no SSM Parameter Store.

CREATE TABLE reading_thresholds (
    id           UUID             NOT NULL PRIMARY KEY,
    reading_type VARCHAR(100)     NOT NULL,
    normal_min   DOUBLE PRECISION,
    normal_max   DOUBLE PRECISION,
    severity     VARCHAR(50)      NOT NULL,
    label        VARCHAR(255),
    created_at   TIMESTAMP        NOT NULL,

    CONSTRAINT uq_reading_thresholds_reading_type UNIQUE (reading_type)
);

-- Sem CREATE INDEX em reading_type: a constraint UNIQUE acima já cria um índice
-- próprio, tanto no PostgreSQL quanto no H2. Um índice adicional na mesma coluna
-- seria redundante e só custaria escrita e espaço.

-- Carga inicial das faixas migradas do SSM Parameter Store.
-- Os ids são literais em vez de função de UUID: gen_random_uuid (PostgreSQL) e
-- random_uuid (H2) não são intercambiáveis, como já documentado na V19.
INSERT INTO reading_thresholds (id, reading_type, normal_min, normal_max, severity, label, created_at) VALUES
    ('7b6f1a2e-0c31-4a5d-9e88-3f2b41d6c001', 'HEART_RATE',  50,   120,  'CRITICAL', 'Frequência cardíaca normal entre 50 e 120 bpm',               CURRENT_TIMESTAMP),
    ('7b6f1a2e-0c31-4a5d-9e88-3f2b41d6c002', 'SPO2',        90,   NULL, 'CRITICAL', 'Saturação de oxigênio normal a partir de 90 por cento',       CURRENT_TIMESTAMP),
    ('7b6f1a2e-0c31-4a5d-9e88-3f2b41d6c003', 'TEMPERATURE', 35.5, 38.0, 'CRITICAL', 'Temperatura corporal normal entre 35,5 e 38,0 graus Celsius', CURRENT_TIMESTAMP);
