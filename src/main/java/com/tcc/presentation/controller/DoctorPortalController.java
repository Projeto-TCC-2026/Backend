package com.tcc.presentation.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.service.PatientProcedureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/doctor")
@Tag(name = "Portal do Médico", description = "Ambiente exclusivo do médico - acesso restrito a DOCTOR")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorPortalController {

    private final PatientProcedureService patientProcedureService;

    public DoctorPortalController(PatientProcedureService patientProcedureService) {
        this.patientProcedureService = patientProcedureService;
    }

    @GetMapping("/procedures")
    @Operation(
        summary = "Listar procedimentos disponíveis para o médico",
        description = "Retorna os procedimentos ativos que o hospital autorizou o médico autenticado a executar. " +
                      "É a lista que pode ser usada ao atribuir um procedimento a um paciente."
    )
    public ResponseEntity<ApiResponse<List<ProcedureResponse>>> listAvailableProcedures(
            Authentication authentication) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                patientProcedureService.listAvailableProcedures(email)));
    }

    // --- Helper ---

    private String extractEmail(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
