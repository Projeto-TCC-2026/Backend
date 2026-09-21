package com.tcc.application.dto.response;

import java.time.LocalDate;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Visão do procedimento atribuído para o próprio paciente.
 *
 * <p>Não expõe {@code patient} — o paciente é sempre o autenticado, seria redundante —
 * nem {@code notes}, que é anotação clínica do médico e não se destina ao paciente.
 */
@Schema(description = "Procedimento atribuído ao paciente autenticado")
public record PatientProcedureSummaryResponse(
        @Schema(description = "Identificador da atribuição do procedimento")
        UUID id,

        @Schema(description = "Procedimento atribuído")
        ProcedureSummary procedure,

        @Schema(description = "Médico responsável pela atribuição")
        DoctorSummary doctor,

        @Schema(description = "Data de início do acompanhamento")
        LocalDate startDate,

        @Schema(description = "Data de término do acompanhamento, quando definida")
        LocalDate endDate,

        @Schema(description = "Situação atual da atribuição", example = "IN_PROGRESS")
        String status
) {}
