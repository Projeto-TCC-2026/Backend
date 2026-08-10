package com.tcc.presentation.controller;

import com.tcc.application.dto.request.PasswordResetRequest;
import com.tcc.application.dto.request.ResetPasswordRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.ForgotPasswordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/forgot-password")
@Tag(name = "Forgot Password", description = "Endpoints de recuperação de senha")
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    public ForgotPasswordController(ForgotPasswordService forgotPasswordService) {
        this.forgotPasswordService = forgotPasswordService;
    }

    @PostMapping("/request")
    @Operation(summary = "Solicitar recuperação de senha", description = "Envia instruções para redefinir a senha de forma genérica")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
        forgotPasswordService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.success(null, "Se o e-mail estiver cadastrado, enviaremos as instruções."));
    }

    @PostMapping("/reset")
    @Operation(summary = "Criar nova senha", description = "Atualiza a senha a partir do token recebido por e-mail")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        forgotPasswordService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Senha atualizada com sucesso."));
    }

}
