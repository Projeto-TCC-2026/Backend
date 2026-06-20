package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.HealthReadingRequest;
import com.tcc.application.dto.response.HealthReadingResponse;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientDevice;
import com.tcc.domain.model.ReadingImport;

@Component
public class HealthReadingMapper {

    private final PatientMapper patientMapper;

    public HealthReadingMapper(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public HealthReadingResponse toResponse(HealthReading healthReading) {
        if (healthReading == null) return null;
        return new HealthReadingResponse(
                healthReading.getId(),
                patientMapper.toSummary(healthReading.getPatient()),
                healthReading.getPatientDevice() != null ? healthReading.getPatientDevice().getId() : null,
                healthReading.getReadingImport() != null ? healthReading.getReadingImport().getId() : null,
                healthReading.getReadingType(),
                healthReading.getValue(),
                healthReading.getUnit(),
                healthReading.getMeasuredAt()
        );
    }

    public HealthReading toEntity(HealthReadingRequest request, Patient patient, PatientDevice patientDevice, ReadingImport readingImport) {
        if (request == null) return null;
        HealthReading healthReading = new HealthReading();
        healthReading.setPatient(patient);
        healthReading.setPatientDevice(patientDevice);
        healthReading.setReadingImport(readingImport);
        healthReading.setReadingType(request.readingType());
        healthReading.setValue(request.value());
        healthReading.setUnit(request.unit());
        healthReading.setMeasuredAt(request.measuredAt());
        return healthReading;
    }

    public void updateEntity(HealthReading healthReading, HealthReadingRequest request, PatientDevice patientDevice, ReadingImport readingImport) {
        healthReading.setPatientDevice(patientDevice);
        healthReading.setReadingImport(readingImport);
        healthReading.setReadingType(request.readingType());
        healthReading.setValue(request.value());
        healthReading.setUnit(request.unit());
        healthReading.setMeasuredAt(request.measuredAt());
    }
}
