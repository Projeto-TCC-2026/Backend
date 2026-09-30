package com.tcc.application.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Contagens agregadas expostas na landing page.
 *
 * <p>Só carrega totais. Nenhum dado de identificação ou de saúde passa por aqui,
 * porque o endpoint que devolve este DTO é público.
 */
@Schema(description = "Estatísticas públicas da plataforma")
public record PublicStatsResponse(

        @Schema(description = "Total de pacientes cadastrados", example = "120")
        long patients,

        @Schema(description = "Total de médicos cadastrados", example = "35")
        long doctors,

        @Schema(description = "Total de hospitais cadastrados", example = "8")
        long hospitals
) {}
