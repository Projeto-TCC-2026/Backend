package com.tcc.application.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.DoctorSummary;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.dto.response.ProcedureSummary;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Procedure;

@Component
public class ProcedureMapper {

    private final DoctorMapper doctorMapper;
    private final HospitalMapper hospitalMapper;

    public ProcedureMapper(DoctorMapper doctorMapper, HospitalMapper hospitalMapper) {
        this.doctorMapper = doctorMapper;
        this.hospitalMapper = hospitalMapper;
    }

    public ProcedureResponse toResponse(Procedure procedure) {
        if (procedure == null) return null;
        return new ProcedureResponse(
                procedure.getId(),
                hospitalMapper.toSummary(procedure.getHospital()),
                procedure.getTitle(),
                procedure.getDescription(),
                procedure.getEstimatedDuration(),
                procedure.getActive(),
                procedure.getCreatedAt(),
                toDoctorSummaries(procedure)
        );
    }

    public ProcedureSummary toSummary(Procedure procedure) {
        if (procedure == null) return null;
        return new ProcedureSummary(
                procedure.getId(),
                procedure.getTitle(),
                procedure.getEstimatedDuration()
        );
    }

    public Procedure toEntity(ProcedureRequest request, Hospital hospital) {
        if (request == null) return null;
        Procedure procedure = new Procedure();
        procedure.setHospital(hospital);
        procedure.setTitle(request.title());
        procedure.setDescription(request.description());
        procedure.setEstimatedDuration(request.estimatedDuration());
        procedure.setActive(request.active() != null ? request.active() : Boolean.TRUE);
        return procedure;
    }

    public void updateEntity(Procedure procedure, ProcedureRequest request) {
        procedure.setTitle(request.title());
        procedure.setDescription(request.description());
        procedure.setEstimatedDuration(request.estimatedDuration());
        if (request.active() != null) {
            procedure.setActive(request.active());
        }
    }

    private List<DoctorSummary> toDoctorSummaries(Procedure procedure) {
        if (procedure.getDoctorProcedures() == null) return List.of();
        return procedure.getDoctorProcedures().stream()
                .map(doctorProcedure -> doctorMapper.toSummary(doctorProcedure.getDoctor()))
                .toList();
    }
}
