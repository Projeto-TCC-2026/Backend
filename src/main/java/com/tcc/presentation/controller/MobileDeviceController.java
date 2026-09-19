package com.tcc.presentation.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.DeviceTokenRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.DeviceTokenService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mobile/devices")
@Tag(name = "Dispositivos Mobile",
     description = "Registro de token de push do aplicativo do paciente")
@PreAuthorize("hasRole('PATIENT')")
public class MobileDeviceController {

    private final DeviceTokenService deviceTokenService;

    public MobileDeviceController(DeviceTokenService deviceTokenService) {
        this.deviceTokenService = deviceTokenService;
    }

    @PostMapping
    @Operation(summary = "Registra o token de push do dispositivo",
               description = "Vincula o token ao usuário autenticado. Se o token já estiver "
                       + "cadastrado, o registro existente é atualizado em vez de duplicado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Token registrado ou atualizado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Dados inválidos"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Perfil sem permissão")
    })
    public ResponseEntity<ApiResponse<Void>> register(
            Authentication authentication,
            @Valid @RequestBody DeviceTokenRequest request) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        deviceTokenService.register(email, request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping
    @Operation(summary = "Remove o token de push do dispositivo",
               description = "Remove o token informado, desde que pertença ao usuário autenticado.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Token removido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "Parâmetro ausente"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Perfil sem permissão"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Token não encontrado para o usuário autenticado")
    })
    public ResponseEntity<ApiResponse<Void>> unregister(
            Authentication authentication,
            @RequestParam String token) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        deviceTokenService.unregister(email, token);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
