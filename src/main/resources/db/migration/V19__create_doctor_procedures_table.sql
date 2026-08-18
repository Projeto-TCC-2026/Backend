-- V20: Tabela de associação doutor-procedimento (N:N)
-- Define quais procedimentos do catálogo do hospital cada médico pode executar.
-- Depende de: doctors, procedures

CREATE TABLE doctor_procedures (
    id           UUID      NOT NULL PRIMARY KEY,
    doctor_id    UUID      NOT NULL,
    procedure_id UUID      NOT NULL,
    created_at   TIMESTAMP NOT NULL,

    CONSTRAINT fk_doctor_procedures_doctor    FOREIGN KEY (doctor_id)    REFERENCES doctors(id),
    CONSTRAINT fk_doctor_procedures_procedure FOREIGN KEY (procedure_id) REFERENCES procedures(id),
    CONSTRAINT uq_doctor_procedures_pair      UNIQUE (doctor_id, procedure_id)
);

-- Backfill: preserva o vínculo atual, em que cada procedimento tinha um único médico dono.
-- O id do vínculo reaproveita o id do procedimento: o backfill gera exatamente uma linha
-- por procedimento, então o valor é único. Evita função de UUID, que difere entre
-- PostgreSQL (gen_random_uuid) e H2 (random_uuid).
INSERT INTO doctor_procedures (id, doctor_id, procedure_id, created_at)
SELECT p.id, p.doctor_id, p.id, CURRENT_TIMESTAMP
FROM procedures p
WHERE p.doctor_id IS NOT NULL;
