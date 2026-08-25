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
    gen_random_uuid (),
    'Número inteiro',
    'Valor numérico sem casas decimais.',
    'INTEGER',
    NULL,
    NULL,
    'number'
  ),
  (
    gen_random_uuid (),
    'Número decimal',
    'Valor numérico com casas decimais.',
    'DECIMAL',
    NULL,
    NULL,
    'number'
  ),
  (
    gen_random_uuid (),
    'Sim ou não',
    'Resposta booleana.',
    'BOOLEAN',
    NULL,
    NULL,
    'toggle'
  ),
  (
    gen_random_uuid (),
    'Texto livre',
    'Resposta aberta do paciente.',
    'TEXT',
    NULL,
    NULL,
    'textarea'
  ),
  (
    gen_random_uuid (),
    'Escala de 0 a 10',
    'Indicador deslizante de intensidade.',
    'SCALE',
    0,
    10,
    'slider'
  ),
  (
    gen_random_uuid (),
    'Foto',
    'Registro fotográfico do acompanhamento.',
    'PHOTO',
    NULL,
    NULL,
    'photo'
  );