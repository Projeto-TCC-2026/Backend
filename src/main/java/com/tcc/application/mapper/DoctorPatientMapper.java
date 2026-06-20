package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.DoctorPatientRequest;
import com.tcc.application.dto.response.DoctorPatientResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import com.tcc.domain.model.Patient;

@Component
public class DoctorPatientMapper {

    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;

    public DoctorPatientMapper(DoctorMapper doctorMapper, PatientMapper patientMapper) {
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
    }

    public DoctorPatientResponse toResponse(DoctorPatient doctorPatient) {
        if (doctorPatient == null) return null;
        return new DoctorPatientResponse(
                doctorPatient.getId(),
                doctorMapper.toSummary(doctorPatient.getDoctor()),
                patientMapper.toSummary(doctorPatient.getPatient()),
                doctorPatient.getCreatedAt()
        );
    }

    public DoctorPatient toEntity(DoctorPatientRequest request, Doctor doctor, Patient patient) {
        if (request == null) return null;
        DoctorPatient doctorPatient = new DoctorPatient();
        doctorPatient.setDoctor(doctor);
        doctorPatient.setPatient(patient);
        return doctorPatient;
    }
}
