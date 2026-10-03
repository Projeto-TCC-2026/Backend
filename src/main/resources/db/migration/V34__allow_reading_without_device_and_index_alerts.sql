-- V34: Ajustes para a leitura que chega pela integração.
--
-- 1) patient_device_id deixa de ser obrigatório.
--    A V12 criou a coluna como NOT NULL porque a leitura era pensada como
--    importação a partir de um dispositivo cadastrado em patient_devices. A
--    leitura que chega em POST /api/integration/alerts/evaluate não carrega
--    dispositivo: o contrato tem apenas patientId, readingType, value e
--    measuredAt. Sem este ALTER a leitura simplesmente não pode ser gravada.
--    Inventar um dispositivo sintético para satisfazer a FK seria pior: criaria
--    linha falsa em patient_devices e mentiria sobre a origem da medição.
--
--    DROP NOT NULL é a sintaxe padrão aceita pelo PostgreSQL e pelo H2 (perfil
--    dev). MODIFY e "ALTER COLUMN <col> NULL" não são portáveis entre os dois.
--
-- 2) Índice para a deduplicação de alertas.
--    A cada leitura fora da faixa o backend consulta se já existe alerta PENDING
--    do mesmo paciente para o mesmo tipo de leitura. O filtro sempre começa por
--    patient_id + status.

ALTER TABLE health_readings
    ALTER COLUMN patient_device_id DROP NOT NULL;

CREATE INDEX idx_alerts_patient_id_status ON alerts (patient_id, status);
