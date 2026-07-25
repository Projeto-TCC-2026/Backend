-- V9: Tabela de execuções reais de procedimentos
-- Depende de: patient_procedures, procedures, doctors, patients

CREATE TABLE procedure_executions (
    id                   UUID        NOT NULL PRIMARY KEY,
    patient_procedure_id UUID        NOT NULL,
    procedure_id         UUID        NOT NULL,
    doctor_id            UUID        NOT NULL,
    patient_id           UUID        NOT NULL,
    execution_date       TIMESTAMP   NOT NULL,
    status               VARCHAR(50) NOT NULL,
    observations         TEXT,

    CONSTRAINT fk_proc_exec_patient_procedure FOREIGN KEY (patient_procedure_id) REFERENCES patient_procedures(id),
    CONSTRAINT fk_proc_exec_procedure         FOREIGN KEY (procedure_id)         REFERENCES procedures(id),
    CONSTRAINT fk_proc_exec_doctor            FOREIGN KEY (doctor_id)            REFERENCES doctors(id),
    CONSTRAINT fk_proc_exec_patient           FOREIGN KEY (patient_id)           REFERENCES patients(id)
);
