CREATE TABLE checkins (
    id                   UUID         NOT NULL PRIMARY KEY,
    patient_procedure_id UUID         NOT NULL,
    source               VARCHAR(20)  NOT NULL,
    submitted_at         TIMESTAMP    NOT NULL,
    manual_date          DATE,

    CONSTRAINT fk_checkins_patient_procedure
        FOREIGN KEY (patient_procedure_id) REFERENCES patient_procedures (id),
    CONSTRAINT uq_checkins_manual_daily
        UNIQUE (patient_procedure_id, manual_date)
);

CREATE TABLE checkin_field_values (
    id         UUID NOT NULL PRIMARY KEY,
    checkin_id UUID NOT NULL,
    field_id   UUID NOT NULL,
    raw_value  TEXT,
    photo_url  VARCHAR(2048),

    CONSTRAINT fk_checkin_field_values_checkin
        FOREIGN KEY (checkin_id) REFERENCES checkins (id),
    CONSTRAINT fk_checkin_field_values_field
        FOREIGN KEY (field_id) REFERENCES doctor_procedure_fields (id),
    CONSTRAINT uq_checkin_field_values_field
        UNIQUE (checkin_id, field_id)
);

CREATE INDEX idx_checkins_submitted_at ON checkins (submitted_at);
CREATE INDEX idx_checkin_field_values_checkin ON checkin_field_values (checkin_id);
