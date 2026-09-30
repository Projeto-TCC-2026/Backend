package com.tcc.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PublicStatsResponse;
import com.tcc.application.service.PublicStatsService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Estatísticas agregadas para a landing page.
 *
 * <p>Endpoint intencionalmente público: só devolve três contagens totais, sem
 * nenhum dado de identificação ou de saúde. Por isso não tem {@code @PreAuthorize}
 * nem {@code @SecurityRequirement} — a liberação correspondente está no
 * {@code SecurityConfig}, restrita ao método GET.
 */
@RestController
@RequestMapping("/api/public")
@Tag(name = "Público", description = "Dados públicos da plataforma, sem autenticação")
public class PublicStatsController {

    private final PublicStatsService publicStatsService;

    public PublicStatsController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Obter estatísticas públicas",
        description = "Retorna o total de pacientes, médicos e hospitais cadastrados. " +
                      "Endpoint público, sem autenticação, e não expõe nenhum dado além dessas contagens."
    )
    public ResponseEntity<ApiResponse<PublicStatsResponse>> getPublicStats() {
        PublicStatsResponse stats = publicStatsService.getPublicStats();
        ApiResponse<PublicStatsResponse> response = ApiResponse.success(stats);

        return ResponseEntity.ok(response);
    }
}
