package com.tcc.application.dto.response;

/**
 * Resposta do cadastro de paciente: dados do paciente + link de ativação (mostrado uma
 * única vez na UI, para o médico repassar caso o e-mail de boas-vindas não chegue).
 * Espelha {@link DoctorRegistrationResponse}.
 */
public record PatientRegistrationResponse(
        PatientResponse patient,
        String activationLink
) {}
