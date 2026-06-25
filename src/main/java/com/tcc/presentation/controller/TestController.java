package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.infrastructure.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/test")
@Tag(name = "Test", description = "Endpoints de teste para validação do JWT")
public class TestController {

    private final JwtService jwtService;

    public TestController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/public")
    @Operation(
        summary = "Endpoint público",
        description = "Endpoint público, não requer autenticação"
    )
    public ResponseEntity<ApiResponse<String>> publicEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("Endpoint público funcionando"));
    }

    @GetMapping("/generate-token")
    @Operation(
        summary = "Gerar token JWT",
        description = "Gera um token JWT para o e-mail informado (apenas para testes)"
    )
    public ResponseEntity<ApiResponse<String>> generateToken(@RequestParam String email) {
        String token = jwtService.generateToken(email);
        return ResponseEntity.ok(ApiResponse.success(token));
    }

    @GetMapping("/validate-token")
    @Operation(
        summary = "Validar token JWT",
        description = "Valida um token JWT e retorna o usuário extraído"
    )
    public ResponseEntity<ApiResponse<String>> validateToken(@RequestParam String token) {
        boolean valid = jwtService.isTokenValid(token);
        String username = jwtService.extractUsername(token);
        String message = valid
                ? "Token válido para o usuário: " + username
                : "Token inválido ou expirado";
        return ResponseEntity.ok(ApiResponse.success(message));
    }
}
