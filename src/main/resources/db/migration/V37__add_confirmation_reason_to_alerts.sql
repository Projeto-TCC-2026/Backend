-- V37: registra POR QUE o alerta foi confirmado.
--
-- Até aqui o motivo da confirmação existia apenas em memória, no evento
-- AlertConfirmedEvent, que define o texto do e-mail ao médico. Depois do commit
-- a informação se perdia: olhando a linha em alerts não havia como saber se o
-- PENDING veio de duas leituras seguidas, da paciente dizendo que não está bem,
-- ou do silêncio dela no prazo de 10 minutos.
--
-- A coluna guarda o nome do enum AlertConfirmationReason como texto:
--   DUAS_LEITURAS          -> duas leituras seguidas fora da faixa, em até 2h
--   PACIENTE_NAO_ESTA_BEM  -> leitura grave e a paciente respondeu NOT_OK
--   SEM_RESPOSTA           -> leitura grave e o prazo venceu sem resposta
--
-- Opcional de propósito. Fica nula em:
--   - alerta que ainda não foi confirmado (UNCONFIRMED, AWAITING_PATIENT);
--   - alerta que nunca vai ser confirmado (NOT_CONFIRMED);
--   - alerta confirmado ANTES desta versão, que não tem o dado em lugar nenhum.
-- Não há backfill: inventar um motivo para alerta antigo seria registrar como
-- fato uma suposição sobre dado clínico. Nulo diz "não se sabe", que é a verdade.
--
-- 30 caracteres cobrem com folga o maior nome do enum (PACIENTE_NAO_ESTA_BEM,
-- 21). Nenhuma coluna existente é alterada.

ALTER TABLE alerts ADD COLUMN confirmation_reason VARCHAR(30);

-- Suporta a listagem de alertas do médico (GET /api/doctor/alerts), que recorta
-- os alertas pelos pacientes vinculados ao médico autenticado. O recorte é um
-- semi-join em doctor_patients por doctor_id, e a tabela só tinha índice na
-- chave primária — em PostgreSQL a chave estrangeira não cria índice sozinha.
CREATE INDEX idx_doctor_patients_doctor_id ON doctor_patients (doctor_id);
