-- V6: Tabela de dispositivos médicos dos pacientes
-- Depende de: patients

CREATE TABLE patient_devices (
    id                UUID         NOT NULL PRIMARY KEY,
    patient_id        UUID         NOT NULL,
    device_identifier VARCHAR(255) NOT NULL,
    device_type       VARCHAR(100) NOT NULL,
    manufacturer      VARCHAR(100),
    model             VARCHAR(100),
    active            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL,

    CONSTRAINT fk_patient_devices_patient FOREIGN KEY (patient_id) REFERENCES patients(id)
);
