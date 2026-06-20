package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.PatientDeviceRequest;
import com.tcc.application.dto.response.PatientDeviceResponse;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientDevice;

@Component
public class PatientDeviceMapper {

    private final PatientMapper patientMapper;

    public PatientDeviceMapper(PatientMapper patientMapper) {
        this.patientMapper = patientMapper;
    }

    public PatientDeviceResponse toResponse(PatientDevice patientDevice) {
        if (patientDevice == null) return null;
        return new PatientDeviceResponse(
                patientDevice.getId(),
                patientMapper.toSummary(patientDevice.getPatient()),
                patientDevice.getDeviceIdentifier(),
                patientDevice.getDeviceType(),
                patientDevice.getManufacturer(),
                patientDevice.getModel(),
                patientDevice.getActive(),
                patientDevice.getCreatedAt()
        );
    }

    public PatientDevice toEntity(PatientDeviceRequest request, Patient patient) {
        if (request == null) return null;
        PatientDevice patientDevice = new PatientDevice();
        patientDevice.setPatient(patient);
        patientDevice.setDeviceIdentifier(request.deviceIdentifier());
        patientDevice.setDeviceType(request.deviceType());
        patientDevice.setManufacturer(request.manufacturer());
        patientDevice.setModel(request.model());
        patientDevice.setActive(request.active() != null ? request.active() : Boolean.TRUE);
        return patientDevice;
    }

    public void updateEntity(PatientDevice patientDevice, PatientDeviceRequest request) {
        patientDevice.setDeviceIdentifier(request.deviceIdentifier());
        patientDevice.setDeviceType(request.deviceType());
        patientDevice.setManufacturer(request.manufacturer());
        patientDevice.setModel(request.model());
        if (request.active() != null) {
            patientDevice.setActive(request.active());
        }
    }
}
