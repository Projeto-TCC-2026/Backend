package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;

@Component
public class PatientProcedureMapper {

    private final PatientMapper patientMapper;
    private final ProcedureMapper procedureMapper;
    private final DoctorMapper doctorMapper;

    public PatientProcedureMapper(PatientMapper patientMapper, ProcedureMapper procedureMapper, DoctorMapper doctorMapper) {
        this.patientMapper = patientMapper;
        this.procedureMapper = procedureMapper;
        this.doctorMapper = doctorMapper;
    }

    public PatientProcedureResponse toResponse(PatientProcedure patientProcedure) {
        if (patientProcedure == null) return null;
        return new PatientProcedureResponse(
                patientProcedure.getId(),
                patientMapper.toSummary(patientProcedure.getPatient()),
                procedureMapper.toSummary(patientProcedure.getProcedure()),
                doctorMapper.toSummary(patientProcedure.getDoctor()),
                patientProcedure.getStartDate(),
                patientProcedure.getEndDate(),
                patientProcedure.getStatus(),
                patientProcedure.getNotes()
        );
    }

    public PatientProcedure toEntity(PatientProcedureRequest request, Patient patient, Procedure procedure, Doctor doctor) {
        if (request == null) return null;
        PatientProcedure patientProcedure = new PatientProcedure();
        patientProcedure.setPatient(patient);
        patientProcedure.setProcedure(procedure);
        patientProcedure.setDoctor(doctor);
        patientProcedure.setStartDate(request.startDate());
        patientProcedure.setEndDate(request.endDate());
        patientProcedure.setStatus(request.status());
        patientProcedure.setNotes(request.notes());
        return patientProcedure;
    }

    public void updateEntity(PatientProcedure patientProcedure, PatientProcedureRequest request) {
        patientProcedure.setStartDate(request.startDate());
        patientProcedure.setEndDate(request.endDate());
        patientProcedure.setStatus(request.status());
        patientProcedure.setNotes(request.notes());
    }
}
