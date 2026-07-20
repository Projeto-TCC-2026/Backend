package com.tcc.application.dto.response;

public record ProceduresByDoctorResponse(
        Long doctorId,
        String doctorName,
        String specialty,
        Long totalProcedures
) {}
