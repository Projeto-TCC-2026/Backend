package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.ProcedureExecutionRequest;
import com.tcc.application.dto.response.ProcedureExecutionResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.ProcedureExecution;

@Component
public class ProcedureExecutionMapper {

    private final ProcedureMapper procedureMapper;
    private final DoctorMapper doctorMapper;
    private final PatientMapper patientMapper;

    public ProcedureExecutionMapper(ProcedureMapper procedureMapper, DoctorMapper doctorMapper, PatientMapper patientMapper) {
        this.procedureMapper = procedureMapper;
        this.doctorMapper = doctorMapper;
        this.patientMapper = patientMapper;
    }

    public ProcedureExecutionResponse toResponse(ProcedureExecution procedureExecution) {
        if (procedureExecution == null) return null;
        return new ProcedureExecutionResponse(
                procedureExecution.getId(),
                procedureExecution.getPatientProcedure() != null ? procedureExecution.getPatientProcedure().getId() : null,
                procedureMapper.toSummary(procedureExecution.getProcedure()),
                doctorMapper.toSummary(procedureExecution.getDoctor()),
                patientMapper.toSummary(procedureExecution.getPatient()),
                procedureExecution.getExecutionDate(),
                procedureExecution.getStatus(),
                procedureExecution.getObservations()
        );
    }

    public ProcedureExecution toEntity(ProcedureExecutionRequest request, PatientProcedure patientProcedure, Procedure procedure, Doctor doctor, Patient patient) {
        if (request == null) return null;
        ProcedureExecution procedureExecution = new ProcedureExecution();
        procedureExecution.setPatientProcedure(patientProcedure);
        procedureExecution.setProcedure(procedure);
        procedureExecution.setDoctor(doctor);
        procedureExecution.setPatient(patient);
        procedureExecution.setExecutionDate(request.executionDate());
        procedureExecution.setStatus(request.status());
        procedureExecution.setObservations(request.observations());
        return procedureExecution;
    }

    public void updateEntity(ProcedureExecution procedureExecution, ProcedureExecutionRequest request) {
        procedureExecution.setExecutionDate(request.executionDate());
        procedureExecution.setStatus(request.status());
        procedureExecution.setObservations(request.observations());
    }
}
