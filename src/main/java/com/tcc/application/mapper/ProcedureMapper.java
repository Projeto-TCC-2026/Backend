package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.dto.response.ProcedureSummary;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Procedure;

@Component
public class ProcedureMapper {

    private final DoctorMapper doctorMapper;

    public ProcedureMapper(DoctorMapper doctorMapper) {
        this.doctorMapper = doctorMapper;
    }

    public ProcedureResponse toResponse(Procedure procedure) {
        if (procedure == null) return null;
        return new ProcedureResponse(
                procedure.getId(),
                doctorMapper.toSummary(procedure.getDoctor()),
                procedure.getTitle(),
                procedure.getDescription(),
                procedure.getEstimatedDuration(),
                procedure.getActive(),
                procedure.getCreatedAt()
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

    public Procedure toEntity(ProcedureRequest request, Doctor doctor) {
        if (request == null) return null;
        Procedure procedure = new Procedure();
        procedure.setDoctor(doctor);
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
}
