-- V35: Filtro de leitura impossível e confirmação de alerta por duas leituras.
--
-- 1) reading_thresholds ganha a faixa PLAUSÍVEL, que é diferente da faixa normal.
--    normal_min/normal_max definem o que é saudável; plausible_min/plausible_max
--    definem o que é fisiologicamente possível de medir. Valor fora da faixa
--    plausível é defeito de sensor, não quadro clínico: a leitura é gravada como
--    suspeita e não avaliada.
--    As colunas são opcionais: tipo sem faixa plausível cadastrada não sofre o
--    filtro, do mesmo jeito que tipo sem faixa normal não gera alerta.
--
-- 2) health_readings.suspect marca a leitura descartada pelo filtro. Obrigatória
--    com padrão false — toda leitura já gravada é, por definição, não suspeita,
--    porque o filtro não existia quando ela entrou.
--
-- 3) alerts.confirmed_at guarda quando o alerta passou de UNCONFIRMED para
--    PENDING. É a partir dela que se conta a janela de 4h que impede repetir o
--    aviso ao médico. Fica nula em alerta ainda não confirmado e nos alertas
--    criados antes desta versão.

ALTER TABLE reading_thresholds ADD COLUMN plausible_min DOUBLE PRECISION;
ALTER TABLE reading_thresholds ADD COLUMN plausible_max DOUBLE PRECISION;

ALTER TABLE health_readings ADD COLUMN suspect BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE alerts ADD COLUMN confirmed_at TIMESTAMP;

-- Faixas plausíveis dos tipos já cadastrados na V26. O UPDATE casa por
-- reading_type, que tem UNIQUE, em vez de pelos ids literais da V26: se alguém
-- tiver recriado as linhas, o filtro continua sendo aplicado.
UPDATE reading_thresholds SET plausible_min = 25,  plausible_max = 250 WHERE reading_type = 'HEART_RATE';
UPDATE reading_thresholds SET plausible_min = 50,  plausible_max = 100 WHERE reading_type = 'SPO2';
UPDATE reading_thresholds SET plausible_min = 30,  plausible_max = 45  WHERE reading_type = 'TEMPERATURE';

-- Suporta a busca da leitura anterior do mesmo paciente e tipo, ignorando as
-- suspeitas, que é feita a cada leitura fora da faixa normal.
CREATE INDEX idx_health_readings_patient_type_measured_at
    ON health_readings (patient_id, reading_type, measured_at);
