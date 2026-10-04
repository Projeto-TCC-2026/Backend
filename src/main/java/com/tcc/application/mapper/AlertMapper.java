package com.tcc.application.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.AlertSummary;
import com.tcc.application.dto.response.DoctorAlertResponse;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;

@Component
public class AlertMapper {

    private final PatientMapper patientMapper;

    public AlertMapper(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public AlertResponse toResponse(Alert alert) {
        if (alert == null) return null;
        return new AlertResponse(
                alert.getId(),
                patientMapper.toSummary(alert.getPatient()),
                alert.getHealthReading() != null ? alert.getHealthReading().getId() : null,
                alert.getSeverity(),
                alert.getTitle(),
                alert.getDescription(),
                alert.getStatus(),
                alert.getCreatedAt()
        );
    }

    /**
     * Converte para a visão do médico, achatando o paciente e a leitura.
     *
     * <p>Exige que {@code patient} e {@code healthReading} já estejam carregados: a
     * consulta que alimenta este mapper os traz por JOIN FETCH, justamente para o
     * acesso aqui não disparar uma consulta por item da página.
     *
     * <p>Alerta sem leitura associada é possível, então os quatro campos derivados
     * dela ficam nulos juntos, em vez de um objeto vazio. Mesmo tratamento de nulo
     * de {@link #toResponse}, que já protege {@code healthReading}.
     */
    public DoctorAlertResponse toDoctorResponse(Alert alert) {
        if (alert == null) return null;

        HealthReading reading = alert.getHealthReading();

        return new DoctorAlertResponse(
                alert.getId(),
                alert.getPatient() != null ? alert.getPatient().getId() : null,
                alert.getPatient() != null ? alert.getPatient().getFullName() : null,
                reading != null ? reading.getReadingType() : null,
                reading != null ? reading.getValue() : null,
                reading != null ? reading.getUnit() : null,
                reading != null ? toUtcOffset(reading.getMeasuredAt()) : null,
                alert.getSeverity(),
                alert.getTitle(),
                alert.getStatus(),
                toUtcOffset(alert.getConfirmedAt()),
                alert.getPatientResponse(),
                alert.getConfirmationReason()
        );
    }

    /**
     * Rotula como UTC o horário lido da coluna, sem deslocar o valor.
     *
     * <p>{@code atOffset} e não {@code atZone}: as colunas de data/hora do projeto
     * guardam UTC em {@code TIMESTAMP} sem fuso, então o valor já está correto e só
     * falta dizer qual é o fuso dele. Qualquer conversão aqui mudaria o instante.
     */
    private OffsetDateTime toUtcOffset(LocalDateTime storedUtc) {
        return storedUtc == null ? null : storedUtc.atOffset(ZoneOffset.UTC);
    }

    public AlertSummary toSummary(Alert alert) {
        if (alert == null) return null;
        return new AlertSummary(
                alert.getId(),
                alert.getSeverity(),
                alert.getTitle(),
                alert.getStatus(),
                alert.getCreatedAt()
        );
    }

    public Alert toEntity(AlertRequest request, Patient patient, HealthReading healthReading) {
        if (request == null) return null;
        Alert alert = new Alert();
        alert.setPatient(patient);
        alert.setHealthReading(healthReading);
        alert.setSeverity(request.severity());
        alert.setTitle(request.title());
        alert.setDescription(request.description());
        alert.setStatus(request.status());
        return alert;
    }

    public void updateEntity(Alert alert, AlertRequest request, HealthReading healthReading) {
        alert.setHealthReading(healthReading);
        alert.setSeverity(request.severity());
        alert.setTitle(request.title());
        alert.setDescription(request.description());
        alert.setStatus(request.status());
    }
}
