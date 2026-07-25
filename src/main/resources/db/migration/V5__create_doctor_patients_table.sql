-- V5: Tabela de associação doutor-paciente (N:N)
-- Depende de: doctors, patients

CREATE TABLE doctor_patients (
    id         UUID      NOT NULL PRIMARY KEY,
    doctor_id  UUID      NOT NULL,
    patient_id UUID      NOT NULL,
    created_at TIMESTAMP NOT NULL,

    CONSTRAINT fk_doctor_patients_doctor  FOREIGN KEY (doctor_id)  REFERENCES doctors(id),
    CONSTRAINT fk_doctor_patients_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);
