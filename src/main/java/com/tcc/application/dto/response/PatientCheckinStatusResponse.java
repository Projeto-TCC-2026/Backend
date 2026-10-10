package com.tcc.application.dto.response;

import java.util.UUID;

/**
 * Status consolidado de check-in diário de um paciente.
 *
 * <p>{@code checkedIn} é {@code true} se o paciente realizou pelo menos um
 * check-in em qualquer procedimento no dia consultado.
 */
public record PatientCheckinStatusResponse(
        UUID patientId,
        String fullName,
        boolean checkedIn
) {}
