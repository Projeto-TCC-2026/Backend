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
import com.tcc.application.dto.response.PatientProcedureSummaryResponse;
import com.tcc.application.service.PatientProcedureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/mobile/patient-procedures")
@Tag(name = "Procedimentos Mobile",
     description = "Consulta dos procedimentos do próprio paciente no aplicativo")
@PreAuthorize("hasRole('PATIENT')")
public class MobilePatientProcedureController {

    private final PatientProcedureService patientProcedureService;

    public MobilePatientProcedureController(PatientProcedureService patientProcedureService) {
        this.patientProcedureService = patientProcedureService;
    }

    @GetMapping
    @Operation(summary = "Lista os procedimentos do paciente autenticado",
               description = "Retorna os procedimentos ativos atribuídos ao paciente do usuário "
                       + "autenticado, do mais recente para o mais antigo pela data de início. "
                       + "O paciente é derivado do token: não há parâmetro de identificação. "
                       + "As anotações clínicas do médico não são expostas.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "Lista de procedimentos, possivelmente vazia"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "Perfil sem permissão"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "Paciente não encontrado para o usuário autenticado")
    })
    public ResponseEntity<ApiResponse<List<PatientProcedureSummaryResponse>>> listOwnProcedures(
            Authentication authentication) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        return ResponseEntity.ok(ApiResponse.success(patientProcedureService.listOwnProcedures(email)));
    }
}
