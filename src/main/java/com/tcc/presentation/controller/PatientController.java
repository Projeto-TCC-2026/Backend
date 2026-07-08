package com.tcc.presentation.controller;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.ProcedureExecutionResponse;
import com.tcc.application.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Pacientes", description = "CRUD de Pacientes - Acesso restrito a Doutores")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Cadastrar novo paciente",
        description = "Criar um novo paciente no sistema. Apenas doutores podem cadastrar pacientes."
    )
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            @Valid @RequestBody PatientRequest request) {
        
        PatientResponse patient = patientService.createPatient(request);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Listar todos os pacientes ativos",
        description = "Retorna uma lista paginada de todos os pacientes ativos cadastrados no sistema. " +
                      "Suporta paginação (page, size) e ordenação (sort)."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> getAllPatients(
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<PatientResponse> patients = patientService.getAllActivePatients(pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar paciente por ID",
        description = "Retorna os dados de um paciente específico pelo seu ID"
    )
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id) {
        
        PatientResponse patient = patientService.getPatientById(id);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Atualizar dados do paciente",
        description = "Atualiza todas as informações de um paciente existente"
    )
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        
        PatientResponse patient = patientService.updatePatient(id, request);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Excluir paciente",
        description = "Remove um paciente do sistema permanentemente (hard delete). " +
                      "Use com cautela, pois esta ação é irreversível."
    )
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id) {
        
        patientService.deletePatient(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/inactive")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Inativar paciente",
        description = "Marca um paciente como inativo no sistema (soft delete)"
    )
    public ResponseEntity<ApiResponse<Void>> inactivatePatient(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id) {
        
        patientService.inactivatePatient(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/name")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por nome",
        description = "Pesquisa pacientes pelo nome completo (busca parcial, case-insensitive). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByName(
            @Parameter(description = "Nome do paciente (busca parcial)", example = "João Silva")
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<PatientResponse> patients = patientService.searchByName(name, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/cpf")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por CPF",
        description = "Pesquisa pacientes pelo CPF (busca parcial). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByCpf(
            @Parameter(description = "CPF do paciente (busca parcial)", example = "123.456.789-00")
            @RequestParam String cpf,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<PatientResponse> patients = patientService.searchByCpf(cpf, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/procedures")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Listar procedimentos realizados de um paciente",
        description = "Retorna todos os procedimentos realizados (ProcedureExecution) associados a um paciente específico. " +
                      "Suporta paginação e ordenação. Preparado para integração futura com o módulo de Procedimentos."
    )
    public ResponseEntity<ApiResponse<Page<ProcedureExecutionResponse>>> getPatientProcedures(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id,
            @PageableDefault(size = 10, sort = "executionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Page<ProcedureExecutionResponse> procedures = patientService.getPatientProcedureExecutions(id, pageable);
        ApiResponse<Page<ProcedureExecutionResponse>> response = ApiResponse.success(procedures);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/procedures/count")
    @PreAuthorize("hasRole('DOCTOR')")
    @Operation(
        summary = "Contar procedimentos realizados de um paciente",
        description = "Retorna o total de procedimentos realizados associados a um paciente específico"
    )
    public ResponseEntity<ApiResponse<Long>> countPatientProcedures(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable Long id) {
        
        Long count = patientService.countPatientProcedureExecutions(id);
        ApiResponse<Long> response = ApiResponse.success(count);
        
        return ResponseEntity.ok(response);
    }
}