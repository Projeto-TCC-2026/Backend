package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Health Check", description = "Endpoints para verificação da saúde da aplicação")
public class HealthController {

    @GetMapping
    @Operation(
        summary = "Verificar status da API",
        description = "Endpoint para verificar se a API está funcionando corretamente"
    )
    public ResponseEntity<ApiResponse<String>> check() {
        return ResponseEntity.ok(ApiResponse.success("API is running"));
    }
}