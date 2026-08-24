package com.tcc.presentation.controller;

import com.tcc.application.dto.request.AccountActivationRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.AccountActivationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/account-activation")
@Tag(name = "Ativação de Conta", description = "Endpoints do fluxo de boas-vindas / primeiro acesso")
public class AccountActivationController {

    private final AccountActivationService accountActivationService;

    public AccountActivationController(AccountActivationService accountActivationService) {
        this.accountActivationService = accountActivationService;
    }

    @PostMapping("/activate")
    @Operation(
        summary = "Definir a primeira senha da conta",
        description = "Valida o token recebido por e-mail de boas-vindas e define a senha da conta recém-criada."
    )
    public ResponseEntity<ApiResponse<Void>> activate(@Valid @RequestBody AccountActivationRequest request) {
        accountActivationService.activateAccount(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Conta ativada com sucesso."));
    }
}
