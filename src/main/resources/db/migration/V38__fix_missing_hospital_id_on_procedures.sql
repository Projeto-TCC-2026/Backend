-- V38: Repara procedures.hospital_id, que a V18 deveria ter criado
--
-- Por que esta migration existe:
-- Em produção a V18 (V18__add_hospital_id_to_procedures.sql) aparece como
-- aplicada no flyway_schema_history, mas o seu SQL nunca rodou — resultado de
-- uma renumeração antiga de migrations seguida de "flyway repair", que
-- reescreveu os checksums e marcou a versão como aplicada sem executá-la.
-- Com isso, a tabela procedures em produção ficou sem a coluna hospital_id,
-- enquanto a entidade Procedure mapeia @JoinColumn(name = "hospital_id",
-- nullable = false). Não se corrige editando a V18: migration já aplicada é
-- imutável, e o histórico de produção jamais a executaria de novo.
--
-- Esta migration é idempotente de propósito, porque precisa atender dois
-- estados de banco diferentes:
--   (a) produção: a coluna não existe  -> cria, torna NOT NULL e cria a FK;
--   (b) banco novo (V18 e V20 rodaram) -> converge para o mesmo estado, sem erro.
-- O estado final é o mesmo que a V18 pretendia, com o mesmo nome de constraint
-- (fk_procedures_hospital), para que os dois ambientes fiquem idênticos.
--
-- Sem backfill: em produção procedures tem 0 linhas, e a fonte usada pela V18
-- (procedures.doctor_id) não existe mais desde a V20. Se algum ambiente tiver
-- linhas com hospital_id nulo, o SET NOT NULL abaixo falha e interrompe a
-- migration — falha explícita é preferível a inventar vínculo de hospital.
--
-- Compatibilidade: ADD COLUMN IF NOT EXISTS, ALTER COLUMN SET NOT NULL e
-- DROP CONSTRAINT IF EXISTS são aceitos por PostgreSQL e por H2 (perfil dev e
-- testes). ADD CONSTRAINT IF NOT EXISTS existe no H2, mas não no PostgreSQL —
-- por isso o par DROP IF EXISTS + ADD, que é a forma portável de tornar a
-- criação da FK repetível.
--
-- Depende de: hospitals (V2), procedures (V7)

ALTER TABLE procedures ADD COLUMN IF NOT EXISTS hospital_id UUID;

ALTER TABLE procedures ALTER COLUMN hospital_id SET NOT NULL;

ALTER TABLE procedures DROP CONSTRAINT IF EXISTS fk_procedures_hospital;

ALTER TABLE procedures ADD CONSTRAINT fk_procedures_hospital FOREIGN KEY (hospital_id) REFERENCES hospitals(id);
