-- V36: Valor GRAVE e pergunta à paciente antes de avisar o médico.
--
-- 1) reading_thresholds ganha a faixa GRAVE, que é uma terceira faixa, diferente
--    da normal e da plausível:
--      normal_min/normal_max      -> o que é saudável
--      plausible_min/plausible_max -> o que é possível de medir (V35)
--      severe_min/severe_max      -> o que é grave o bastante para perguntar à
--                                    paciente na hora, em vez de esperar a
--                                    segunda leitura do fluxo comum
--    Diferente das outras duas, os limites graves são inclusivos POR DENTRO:
--    é grave quando valor <= severe_min ou valor >= severe_max. Por isso 40 e
--    131 são graves para HEART_RATE, mas 41 e 130 não.
--    As colunas são opcionais: tipo sem faixa grave cadastrada segue apenas o
--    fluxo comum de confirmação por duas leituras.
--
-- 2) alerts ganha o ciclo da pergunta à paciente:
--      patient_response_deadline -> até quando a resposta é aceita (criação + 10 min)
--      patient_response          -> 'OK' ou 'NOT_OK', nulo enquanto não responde
--      patient_responded_at      -> quando a resposta chegou
--    As três ficam nulas em alerta do fluxo comum e nos alertas criados antes
--    desta versão. Nenhuma coluna existente é alterada.

ALTER TABLE reading_thresholds ADD COLUMN severe_min DOUBLE PRECISION;
ALTER TABLE reading_thresholds ADD COLUMN severe_max DOUBLE PRECISION;

ALTER TABLE alerts ADD COLUMN patient_response_deadline TIMESTAMP;
ALTER TABLE alerts ADD COLUMN patient_response VARCHAR(20);
ALTER TABLE alerts ADD COLUMN patient_responded_at TIMESTAMP;

-- Faixas graves dos tipos já cadastrados na V26. O UPDATE casa por reading_type,
-- que tem UNIQUE, em vez de pelos ids literais da V26 — mesmo critério da V35.
--
-- SPO2 e TEMPERATURE não recebem severe_max: saturação alta e temperatura alta
-- não são o quadro que justifica interromper a paciente com uma pergunta. Sem
-- limite daquele lado, a coluna fica nula e o lado é ignorado.
UPDATE reading_thresholds SET severe_min = 40,   severe_max = 131  WHERE reading_type = 'HEART_RATE';
UPDATE reading_thresholds SET severe_min = 84,   severe_max = NULL WHERE reading_type = 'SPO2';
UPDATE reading_thresholds SET severe_min = 35.0, severe_max = NULL WHERE reading_type = 'TEMPERATURE';

-- Suporta a varredura do agendador, que roda a cada minuto procurando alerta
-- AWAITING_PATIENT com prazo vencido.
CREATE INDEX idx_alerts_status_patient_response_deadline
    ON alerts (status, patient_response_deadline);
