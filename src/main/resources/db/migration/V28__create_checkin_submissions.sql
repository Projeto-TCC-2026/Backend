CREATE TABLE checkin_submissions (
    id                   UUID         NOT NULL PRIMARY KEY,
    patient_id           UUID         NOT NULL,
    idempotency_key      VARCHAR(128) NOT NULL,
    configuration_version VARCHAR(128),
    submitted_at         TIMESTAMP    NOT NULL,
    updated_at           TIMESTAMP    NOT NULL,
    edit_until           TIMESTAMP    NOT NULL,

    CONSTRAINT fk_checkin_submissions_patient
        FOREIGN KEY (patient_id) REFERENCES patients (id),
    CONSTRAINT uq_checkin_submissions_patient_idempotency
        UNIQUE (patient_id, idempotency_key)
);

ALTER TABLE checkins
    ADD COLUMN submission_id UUID;

ALTER TABLE checkins
    ADD CONSTRAINT fk_checkins_submission
        FOREIGN KEY (submission_id) REFERENCES checkin_submissions (id);

CREATE INDEX idx_checkins_submission_id ON checkins (submission_id);
