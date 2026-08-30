CREATE TABLE
  field_type_presets (
    id UUID NOT NULL PRIMARY KEY,
    name VARCHAR(120) NOT NULL UNIQUE,
    description TEXT,
    data_type VARCHAR(20) NOT NULL,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    input_style VARCHAR(40) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
  );

CREATE TABLE
  doctor_procedure_fields (
    id UUID NOT NULL PRIMARY KEY,
    doctor_procedure_id UUID NOT NULL,
    name VARCHAR(120) NOT NULL,
    description TEXT,
    unit VARCHAR(40),
    data_type VARCHAR(20) NOT NULL,
    metric_key VARCHAR(60),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT NOT NULL DEFAULT 0,
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT fk_doctor_procedure_fields_assignment FOREIGN KEY (doctor_procedure_id) REFERENCES doctor_procedures (id)
  );

CREATE TABLE
  field_thresholds (
    id UUID NOT NULL PRIMARY KEY,
    field_id UUID NOT NULL,
    severity_order INT NOT NULL,
    label VARCHAR(80) NOT NULL,
    color VARCHAR(30),
    min_value DOUBLE PRECISION,
    max_value DOUBLE PRECISION,
    CONSTRAINT fk_field_thresholds_field FOREIGN KEY (field_id) REFERENCES doctor_procedure_fields (id)
  );

CREATE INDEX idx_doctor_procedure_fields_assignment ON doctor_procedure_fields (doctor_procedure_id, active, display_order);

CREATE INDEX idx_field_thresholds_field ON field_thresholds (field_id);

INSERT INTO
  field_type_presets (
    id,
    name,
    description,
    data_type,
    min_value,
    max_value,
    input_style
  )
VALUES
  (
    'a3f1e2c4-7b6d-4a8e-9c1f-2d3b4a5e6f70',
    'Número inteiro',
    'Valor numérico sem casas decimais.',
    'INTEGER',
    NULL,
    NULL,
    'number'
  ),
  (
    'b4e2f3d5-8c7e-4b9f-ad2e-3e4c5b6f7a81',
    'Número decimal',
    'Valor numérico com casas decimais.',
    'DECIMAL',
    NULL,
    NULL,
    'number'
  ),
  (
    'c5f3a4e6-9d8f-4c0a-be3f-4f5d6c7a8b92',
    'Sim ou não',
    'Resposta booleana.',
    'BOOLEAN',
    NULL,
    NULL,
    'toggle'
  ),
  (
    'd6a4b5f7-ae9a-4d1b-cf40-5a6e7d8b9ca3',
    'Texto livre',
    'Resposta aberta do paciente.',
    'TEXT',
    NULL,
    NULL,
    'textarea'
  ),
  (
    'e7b5c6a8-bf0b-4e2c-d051-6b7f8e9cadb4',
    'Escala de 0 a 10',
    'Indicador deslizante de intensidade.',
    'SCALE',
    0,
    10,
    'slider'
  ),
  (
    'f8c6d7b9-c01c-4f3d-e162-7c809fadbec5',
    'Foto',
    'Registro fotográfico do acompanhamento.',
    'PHOTO',
    NULL,
    NULL,
    'photo'
  );