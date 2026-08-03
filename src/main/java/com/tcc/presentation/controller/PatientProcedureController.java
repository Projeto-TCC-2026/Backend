package com.tcc.presentation.controller;

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
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.application.service.PatientProcedureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/patients/{patientId}/assigned-procedures")
@Tag(name = "Procedimentos Atribuídos ao Paciente",
     description = "Atribuição de procedimentos a pacientes - acesso restrito a DOCTOR")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('DOCTOR')")
public class PatientProcedureController {

    private final PatientProcedureService patientProcedureService;

    public PatientProcedureController(PatientProcedureService patientProcedureService) {
        this.patientProcedureService = patientProcedureService;
    }

    @PostMapping
    @Operation(
        summary = "Atribuir procedimento ao paciente",
        description = "Vincula um procedimento ao paciente. O médico é derivado do token e só pode atribuir " +
                      "procedimentos ativos que o hospital autorizou para ele, em pacientes vinculados a ele."
    )
    public ResponseEntity<ApiResponse<PatientProcedureResponse>> assignProcedure(
            Authentication authentication,
            @Parameter(description = "ID do paciente", required = true) @PathVariable UUID patientId,
            @Valid @RequestBody PatientProcedureRequest request) {
        String email = extractEmail(authentication);
        PatientProcedureResponse assignment =
                patientProcedureService.assignProcedure(email, patientId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(assignment));
    }

    @GetMapping
    @Operation(
        summary = "Listar procedimentos atribuídos ao paciente",
        description = "Retorna lista paginada dos procedimentos que o médico autenticado atribuiu ao paciente"
    )
    public ResponseEntity<ApiResponse<Page<PatientProcedureResponse>>> listPatientProcedures(
            Authentication authentication,
            @Parameter(description = "ID do paciente", required = true) @PathVariable UUID patientId,
            @PageableDefault(size = 10, sort = "startDate", direction = Sort.Direction.DESC) Pageable pageable) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                patientProcedureService.listPatientProcedures(email, patientId, pageable)));
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Atualizar atribuição",
        description = "Atualiza datas, status e observações da atribuição. O procedimento vinculado não muda: " +
                      "para trocar de procedimento, remova a atribuição e crie outra."
    )
    public ResponseEntity<ApiResponse<PatientProcedureResponse>> updateAssignment(
            Authentication authentication,
            @Parameter(description = "ID do paciente", required = true) @PathVariable UUID patientId,
            @Parameter(description = "ID da atribuição", required = true) @PathVariable UUID id,
            @Valid @RequestBody PatientProcedureRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(
                patientProcedureService.updateAssignment(email, patientId, id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "Remover atribuição",
        description = "Remove o vínculo entre procedimento e paciente. Só remove atribuição feita pelo " +
                      "próprio médico autenticado."
    )
    public ResponseEntity<ApiResponse<Void>> removeAssignment(
            Authentication authentication,
            @Parameter(description = "ID do paciente", required = true) @PathVariable UUID patientId,
            @Parameter(description = "ID da atribuição", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        patientProcedureService.removeAssignment(email, patientId, id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // --- Helper ---

    private String extractEmail(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
