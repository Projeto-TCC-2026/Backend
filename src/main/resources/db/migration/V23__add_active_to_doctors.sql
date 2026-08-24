ALTER TABLE doctors
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

CREATE INDEX idx_doctors_active ON doctors(active);
