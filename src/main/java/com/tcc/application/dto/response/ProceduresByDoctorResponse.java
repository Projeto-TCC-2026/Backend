package com.tcc.application.dto.response;

import java.util.UUID;

public record ProceduresByDoctorResponse(
        UUID doctorId,
        String doctorName,
        String specialty,
        Long totalProcedures
) {}
