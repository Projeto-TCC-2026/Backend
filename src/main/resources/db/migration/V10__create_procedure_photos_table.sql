-- V10: Tabela de fotos de execuções de procedimentos
-- Depende de: procedure_executions

CREATE TABLE procedure_photos (
    id                     UUID         NOT NULL PRIMARY KEY,
    procedure_execution_id UUID         NOT NULL,
    image_url              VARCHAR(500) NOT NULL,
    file_name              VARCHAR(255) NOT NULL,
    uploaded_at            TIMESTAMP    NOT NULL,

    CONSTRAINT fk_procedure_photos_execution FOREIGN KEY (procedure_execution_id) REFERENCES procedure_executions(id)
);
