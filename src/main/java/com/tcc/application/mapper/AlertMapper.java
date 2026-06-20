package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.AlertRequest;
import com.tcc.application.dto.response.AlertResponse;
import com.tcc.application.dto.response.AlertSummary;
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
