package com.tcc.presentation.controller;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.service.DoctorService;
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
@RequestMapping("/api/doctors")
@Tag(name = "Doutores", description = "CRUD de Doutores - Gerenciamento de doutores do sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Cadastrar novo doutor",
        description = "Criar um novo doutor no sistema. Apenas administradores podem cadastrar doutores. " +
                      "O doutor deve estar associado a um hospital existente e a um usuário válido."
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            @Valid @RequestBody DoctorRequest request) {
        
        DoctorResponse doctor = doctorService.createDoctor(request);
        ApiResponse<DoctorResponse> response = ApiResponse.success(doctor);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Listar todos os doutores",
        description = "Retorna uma lista paginada de todos os doutores cadastrados no sistema. " +
                      "Suporta paginação (page, size) e ordenação (sort). " +
                      "Inclui informações do hospital associado."
    )
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> getAllDoctors(
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<DoctorResponse> doctors = doctorService.getAllDoctors(pageable);
        ApiResponse<Page<DoctorResponse>> response = ApiResponse.success(doctors);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar doutor por ID",
        description = "Retorna os dados de um doutor específico pelo seu ID, incluindo informações do hospital associado"
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(
            @Parameter(description = "ID do doutor", example = "1", required = true)
            @PathVariable Long id) {
        
        DoctorResponse doctor = doctorService.getDoctorById(id);
        ApiResponse<DoctorResponse> response = ApiResponse.success(doctor);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Atualizar dados do doutor",
        description = "Atualiza todas as informações de um doutor existente. " +
                      "O hospital informado deve existir no sistema."
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            @Parameter(description = "ID do doutor", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {
        
        DoctorResponse doctor = doctorService.updateDoctor(id, request);
        ApiResponse<DoctorResponse> response = ApiResponse.success(doctor);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Excluir doutor",
        description = "Remove um doutor do sistema. Apenas administradores podem excluir doutores. " +
                      "Não é possível excluir se houver pacientes ou procedimentos associados."
    )
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(
            @Parameter(description = "ID do doutor", example = "1", required = true)
            @PathVariable Long id) {
        
        doctorService.deleteDoctor(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/name")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar doutores por nome",
        description = "Pesquisa doutores pelo nome completo (busca parcial, case-insensitive). " +
                      "Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> searchByName(
            @Parameter(description = "Nome do doutor (busca parcial)", example = "João Silva")
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<DoctorResponse> doctors = doctorService.searchByName(name, pageable);
        ApiResponse<Page<DoctorResponse>> response = ApiResponse.success(doctors);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/crm")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar doutor por CRM",
        description = "Pesquisa um doutor específico pelo número de CRM (busca exata)"
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> searchByCrm(
            @Parameter(description = "Número do CRM", example = "12345-SP", required = true)
            @RequestParam String crm) {
        
        DoctorResponse doctor = doctorService.searchByCrm(crm);
        ApiResponse<DoctorResponse> response = ApiResponse.success(doctor);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/specialty")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar doutores por especialidade",
        description = "Pesquisa doutores pela especialidade (busca parcial, case-insensitive). " +
                      "Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> searchBySpecialty(
            @Parameter(description = "Especialidade médica", example = "Cardiologia")
            @RequestParam String specialty,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<DoctorResponse> doctors = doctorService.searchBySpecialty(specialty, pageable);
        ApiResponse<Page<DoctorResponse>> response = ApiResponse.success(doctors);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Filtrar doutores",
        description = "Filtra doutores por múltiplos critérios: hospital, especialidade, nome e CRM. " +
                      "Todos os parâmetros são opcionais e combinados com AND. " +
                      "Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> filterDoctors(
            @Parameter(description = "ID do hospital")
            @RequestParam(required = false) Long hospitalId,
            
            @Parameter(description = "Especialidade médica (busca parcial)")
            @RequestParam(required = false) String specialty,
            
            @Parameter(description = "Nome do doutor (busca parcial)")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Número do CRM (busca parcial)")
            @RequestParam(required = false) String crm,
            
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<DoctorResponse> doctors = doctorService.filterDoctors(hospitalId, specialty, name, crm, pageable);
        ApiResponse<Page<DoctorResponse>> response = ApiResponse.success(doctors);
        
        return ResponseEntity.ok(response);
    }
}
