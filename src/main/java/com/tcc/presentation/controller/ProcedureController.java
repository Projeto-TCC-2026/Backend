package com.tcc.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.DoctorProcedureRequest;
import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.service.ProcedureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/hospital/procedures")
@Tag(name = "Procedimentos do Hospital", description = "Catálogo de procedimentos - acesso restrito a HOSPITAL")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('HOSPITAL')")
public class ProcedureController {

    private final ProcedureService procedureService;

    public ProcedureController(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @PostMapping
    @Operation(
        summary = "Cadastrar procedimento",
        description = "Cria um procedimento no catálogo do hospital vinculado ao usuário autenticado. " +
                      "O título não pode repetir outro procedimento do mesmo hospital."
    )
    public ResponseEntity<ApiResponse<ProcedureResponse>> createProcedure(
            Authentication authentication,
            @Valid @RequestBody ProcedureRequest request) {
        String email = extractEmail(authentication);
        ProcedureResponse procedure = procedureService.createProcedure(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(procedure));
    }

    @GetMapping
    @Operation(
        summary = "Listar procedimentos do hospital",
        description = "Retorna lista paginada dos procedimentos do hospital do usuário autenticado. " +
                      "Por padrão retorna apenas os ativos; use includeInactive=true para incluir os inativos."
    )
    public ResponseEntity<ApiResponse<Page<ProcedureResponse>>> listProcedures(
            Authentication authentication,
            @Parameter(description = "Inclui procedimentos inativos na listagem")
            @RequestParam(defaultValue = "false") Boolean includeInactive,
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        String email = extractEmail(authentication);
        Page<ProcedureResponse> procedures = procedureService.listProcedures(email, includeInactive, pageable);
        return ResponseEntity.ok(ApiResponse.success(procedures));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Buscar procedimento por ID",
        description = "Retorna os dados de um procedimento, validando que pertence ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<ProcedureResponse>> getProcedureById(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(procedureService.getProcedureById(email, id)));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar procedimento",
        description = "Atualiza um procedimento, validando que pertence ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<ProcedureResponse>> updateProcedure(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id,
            @Valid @RequestBody ProcedureRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(procedureService.updateProcedure(email, id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Inativar procedimento",
        description = "Inativa o procedimento (active = false), validando que pertence ao hospital do usuário " +
                      "autenticado. O registro é mantido para preservar o histórico de pacientes e execuções."
    )
    public ResponseEntity<ApiResponse<Void>> deactivateProcedure(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        procedureService.deactivateProcedure(email, id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/doctors")
    @Operation(
        summary = "Listar médicos do procedimento",
        description = "Retorna os médicos autorizados a executar o procedimento, " +
                      "validando que ele pertence ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<List<DoctorProcedureResponse>>> listProcedureDoctors(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(procedureService.listProcedureDoctors(email, id)));
    }

    @GetMapping("/doctors/{doctorId}")
    @Operation(summary = "Listar procedimentos do médico")
    public ResponseEntity<ApiResponse<List<DoctorProcedureResponse>>> listDoctorProcedures(
            Authentication authentication,
            @PathVariable UUID doctorId) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(procedureService.listDoctorProcedures(email, doctorId)));
    }

    @PostMapping("/{id}/doctors")
    @Operation(
        summary = "Atrelar médico ao procedimento",
        description = "Autoriza um médico do hospital a executar o procedimento. " +
                      "Procedimento e médico precisam pertencer ao hospital do usuário autenticado."
    )
    public ResponseEntity<ApiResponse<DoctorProcedureResponse>> assignDoctor(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id,
            @Valid @RequestBody DoctorProcedureRequest request) {
        String email = extractEmail(authentication);
        DoctorProcedureResponse assignment = procedureService.assignDoctor(email, id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assignment));
    }

    @DeleteMapping("/{id}/doctors/{doctorId}")
    @Operation(
        summary = "Desatrelar médico do procedimento",
        description = "Remove a autorização do médico para executar o procedimento. " +
                      "Procedimento e médico precisam pertencer ao hospital do usuário autenticado."
    )
    public ResponseEntity<ApiResponse<Void>> unassignDoctor(
            Authentication authentication,
            @Parameter(description = "ID do procedimento", required = true) @PathVariable UUID id,
            @Parameter(description = "ID do médico", required = true) @PathVariable UUID doctorId) {
        String email = extractEmail(authentication);
        procedureService.unassignDoctor(email, id, doctorId);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // --- Helper ---

    private String extractEmail(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
