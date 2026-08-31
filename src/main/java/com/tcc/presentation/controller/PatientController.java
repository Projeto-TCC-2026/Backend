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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/api/patients")
@Tag(name = "Pacientes", description = "CRUD de Pacientes — ADMIN, HOSPITAL e DOCTOR")
@SecurityRequirement(name = "Bearer Authentication")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Cadastrar novo paciente",
        description = "Criar um novo paciente no sistema e vinculá-lo ao médico autenticado. " +
                      "Doutores e Administradores podem cadastrar pacientes, e o paciente criado fica sob " +
                      "responsabilidade de quem o cadastrou."
    )
    public ResponseEntity<ApiResponse<PatientResponse>> createPatient(
            Authentication authentication,
            @Valid @RequestBody PatientRequest request) {

        String email = extractEmail(authentication);
        PatientResponse patient = patientService.createPatient(email, request);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Listar pacientes visíveis ao perfil autenticado",
        description = "ADMIN vê todos os pacientes ativos. HOSPITAL vê apenas pacientes vinculados a médicos do próprio hospital. DOCTOR vê apenas os seus pacientes."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> getAllPatients(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.getAllActivePatients(extractEmail(authentication), pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Buscar paciente por ID",
        description = "Retorna os dados de um paciente específico pelo seu ID"
    )
    public ResponseEntity<ApiResponse<PatientResponse>> getPatientById(
            Authentication authentication,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id) {

        PatientResponse patient = patientService.getPatientById(extractEmail(authentication), id);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Atualizar dados do paciente",
        description = "Atualiza todas as informações de um paciente existente"
    )
    public ResponseEntity<ApiResponse<PatientResponse>> updatePatient(
            Authentication authentication,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody PatientRequest request) {

        PatientResponse patient = patientService.updatePatient(extractEmail(authentication), id, request);
        ApiResponse<PatientResponse> response = ApiResponse.success(patient);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Excluir paciente",
        description = "Remove um paciente do sistema permanentemente (hard delete). " +
                      "Apenas Administradores podem excluir pacientes. Use com cautela, pois esta ação é irreversível."
    )
    public ResponseEntity<ApiResponse<Void>> deletePatient(
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id) {
        
        patientService.deletePatient(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/inactive")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Inativar paciente",
        description = "Marca um paciente como inativo no sistema (soft delete)"
    )
    public ResponseEntity<ApiResponse<Void>> inactivatePatient(
            Authentication authentication,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id) {

        patientService.inactivatePatient(extractEmail(authentication), id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/name")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por nome",
        description = "Pesquisa pacientes pelo nome completo (busca parcial, case-insensitive). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByName(
            Authentication authentication,
            @Parameter(description = "Nome do paciente (busca parcial)", example = "João Silva")
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.searchByName(extractEmail(authentication), name, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/cpf")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por CPF",
        description = "Pesquisa pacientes pelo CPF (busca parcial). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByCpf(
            Authentication authentication,
            @Parameter(description = "CPF do paciente (busca parcial)", example = "123.456.789-00")
            @RequestParam String cpf,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.searchByCpf(extractEmail(authentication), cpf, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/email")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por e-mail",
        description = "Pesquisa pacientes pelo e-mail (busca parcial, case-insensitive). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByEmail(
            Authentication authentication,
            @Parameter(description = "E-mail do paciente (busca parcial)", example = "joao@email.com")
            @RequestParam String email,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.searchByEmail(extractEmail(authentication), email, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/phone")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Buscar pacientes por telefone",
        description = "Pesquisa pacientes pelo telefone (busca parcial). " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> searchByPhone(
            Authentication authentication,
            @Parameter(description = "Telefone do paciente (busca parcial)", example = "11987654321")
            @RequestParam String phone,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.searchByPhone(extractEmail(authentication), phone, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Filtrar pacientes",
        description = "Filtra pacientes por múltiplos critérios: nome, gênero, cidade e estado. " +
                      "Todos os parâmetros são opcionais e combinados com AND. " +
                      "Retorna apenas pacientes ativos. Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<PatientResponse>>> filterPatients(
            Authentication authentication,
            @Parameter(description = "Nome do paciente (busca parcial)")
            @RequestParam(required = false) String name,

            @Parameter(description = "Gênero do paciente", example = "Masculino")
            @RequestParam(required = false) String gender,

            @Parameter(description = "Cidade do paciente")
            @RequestParam(required = false) String city,

            @Parameter(description = "Estado do paciente (sigla de 2 letras)", example = "SP")
            @RequestParam(required = false) String state,

            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {

        Page<PatientResponse> patients = patientService.filterPatients(
                extractEmail(authentication), name, gender, city, state, pageable);
        ApiResponse<Page<PatientResponse>> response = ApiResponse.success(patients);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/procedures")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Listar procedimentos realizados de um paciente",
        description = "Retorna todos os procedimentos realizados (ProcedureExecution) associados a um paciente específico. " +
                      "Suporta paginação e ordenação. Preparado para integração futura com o módulo de Procedimentos."
    )
    public ResponseEntity<ApiResponse<Page<ProcedureExecutionResponse>>> getPatientProcedures(
            Authentication authentication,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id,
            @PageableDefault(size = 10, sort = "executionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<ProcedureExecutionResponse> procedures = patientService.getPatientProcedureExecutions(
                extractEmail(authentication), id, pageable);
        ApiResponse<Page<ProcedureExecutionResponse>> response = ApiResponse.success(procedures);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/procedures/count")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOSPITAL', 'DOCTOR')")
    @Operation(
        summary = "Contar procedimentos realizados de um paciente",
        description = "Retorna o total de procedimentos realizados associados a um paciente específico"
    )
    public ResponseEntity<ApiResponse<Long>> countPatientProcedures(
            Authentication authentication,
            @Parameter(description = "ID do paciente", example = "1", required = true)
            @PathVariable UUID id) {

        Long count = patientService.countPatientProcedureExecutions(extractEmail(authentication), id);
        ApiResponse<Long> response = ApiResponse.success(count);
        
        return ResponseEntity.ok(response);
    }

    // --- Helper ---

    private String extractEmail(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}