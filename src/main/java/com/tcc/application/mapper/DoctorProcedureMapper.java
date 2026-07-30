package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.Procedure;

@Component
public class DoctorProcedureMapper {

    private final DoctorMapper doctorMapper;
    private final ProcedureMapper procedureMapper;

    public DoctorProcedureMapper(DoctorMapper doctorMapper, ProcedureMapper procedureMapper) {
        this.doctorMapper = doctorMapper;
        this.procedureMapper = procedureMapper;
    }

    public DoctorProcedureResponse toResponse(DoctorProcedure doctorProcedure) {
        if (doctorProcedure == null) return null;
        return new DoctorProcedureResponse(
                doctorProcedure.getId(),
                doctorMapper.toSummary(doctorProcedure.getDoctor()),
                procedureMapper.toSummary(doctorProcedure.getProcedure()),
                doctorProcedure.getCreatedAt()
        );
    }

    public DoctorProcedure toEntity(Doctor doctor, Procedure procedure) {
        DoctorProcedure doctorProcedure = new DoctorProcedure();
        doctorProcedure.setDoctor(doctor);
        doctorProcedure.setProcedure(procedure);
        return doctorProcedure;
    }
}
