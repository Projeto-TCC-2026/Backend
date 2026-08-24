package com.tcc.application.dto.response;

/** Resposta do cadastro de um doutor com conta nova: dados do doutor + link de ativação (mostrado uma única vez na UI). */
public record DoctorRegistrationResponse(
        DoctorResponse doctor,
        String activationLink
) {}
