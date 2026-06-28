package com.tcc.presentation.controller;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.service.PatientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
        description = "Retorna uma lista de todos os pacientes ativos cadastrados no sistema"
    )
    public ResponseEntity<ApiResponse<List<PatientResponse>>> getAllPatients() {
        List<PatientResponse> patients = patientService.getAllActivePatients();
        ApiResponse<List<PatientResponse>> response = ApiResponse.success(patients);
        
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
}