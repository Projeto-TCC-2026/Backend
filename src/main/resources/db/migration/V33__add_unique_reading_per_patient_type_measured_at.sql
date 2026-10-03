-- V33: Idempotência da leitura de saúde vinda da integração.
-- A fila que alimenta POST /api/integration/alerts/evaluate pode reentregar a
-- mesma mensagem. A unicidade abaixo é a garantia final: mesmo que duas
-- requisições idênticas cheguem em paralelo e as duas passem pela checagem da
-- aplicação, o banco recusa a segunda inserção.
--
-- A chave é (patient_id, reading_type, measured_at): é o que identifica uma
-- medição do ponto de vista de quem envia. patient_device_id fica fora de
-- propósito — o mesmo sinal reentregue não pode virar duas leituras só porque
-- o dispositivo resolvido mudou.
--
-- Nenhuma linha é escrita em health_readings hoje (a V12 criou a tabela e não
-- existe código que insira nela), então não há duplicata pré-existente para
-- limpar antes de aplicar a restrição.

ALTER TABLE health_readings
    ADD CONSTRAINT uq_health_readings_patient_type_measured_at
    UNIQUE (patient_id, reading_type, measured_at);
