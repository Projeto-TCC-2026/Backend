package com.tcc.presentation.controller;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.service.HospitalService;
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
@RequestMapping("/api/hospitals")
@Tag(name = "Hospitais", description = "CRUD de Hospitais - Gerenciamento de hospitais do sistema")
@SecurityRequirement(name = "Bearer Authentication")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Cadastrar novo hospital",
        description = "Criar um novo hospital no sistema. Apenas administradores e doutores podem cadastrar hospitais."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.createHospital(request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Listar todos os hospitais",
        description = "Retorna uma lista paginada de todos os hospitais cadastrados no sistema. " +
                      "Suporta paginação (page, size) e ordenação (sort)."
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> getAllHospitals(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.getAllHospitals(pageable);
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar hospital por ID",
        description = "Retorna os dados de um hospital específico pelo seu ID"
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Atualizar dados do hospital",
        description = "Atualiza todas as informações de um hospital existente"
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.updateHospital(id, request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Excluir hospital",
        description = "Remove um hospital do sistema. Apenas administradores podem excluir hospitais. " +
                      "Não é possível excluir se houver doutores associados."
    )
    public ResponseEntity<ApiResponse<Void>> deleteHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        hospitalService.deleteHospital(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search/name")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Buscar hospitais por nome",
        description = "Pesquisa hospitais pelo nome (busca parcial, case-insensitive). " +
                      "Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> searchByName(
            @Parameter(description = "Nome do hospital (busca parcial)", example = "Santa Casa")
            @RequestParam String name,
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.searchByName(name, pageable);
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/filter")
    @PreAuthorize("hasRole('ADMIN') or hasRole('DOCTOR')")
    @Operation(
        summary = "Filtrar hospitais",
        description = "Filtra hospitais por múltiplos critérios: nome, cidade e estado. " +
                      "Todos os parâmetros são opcionais e combinados com AND. " +
                      "Suporta paginação e ordenação."
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> filterHospitals(
            @Parameter(description = "Nome do hospital (busca parcial)")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Cidade do hospital")
            @RequestParam(required = false) String city,
            
            @Parameter(description = "Estado do hospital (sigla de 2 letras)", example = "SP")
            @RequestParam(required = false) String state,
            
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.filterHospitals(name, city, state, pageable);
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        
        return ResponseEntity.ok(response);
    }
}
