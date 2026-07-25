-- V14: Tabela de notificações enviadas aos doutores
-- Depende de: alerts, doctors

CREATE TABLE notifications (
    id        UUID        NOT NULL PRIMARY KEY,
    alert_id  UUID        NOT NULL,
    doctor_id UUID        NOT NULL,
    message   TEXT        NOT NULL,
    sent_at   TIMESTAMP   NOT NULL,
    read_at   TIMESTAMP,
    status    VARCHAR(50) NOT NULL,

    CONSTRAINT fk_notifications_alert  FOREIGN KEY (alert_id)  REFERENCES alerts(id),
    CONSTRAINT fk_notifications_doctor FOREIGN KEY (doctor_id) REFERENCES doctors(id)
);
