package com.tcc.presentation.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.HospitalSummary;
import com.tcc.application.service.HospitalService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/hospital-management")
@Tag(name = "Gestão de Hospitais", description = "Módulo completo para gestão administrativa de hospitais")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class HospitalManagementController {

    private final HospitalService hospitalService;

    public HospitalManagementController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    // ======================= LISTAGEM E VISUALIZAÇÃO =======================

    @GetMapping
    @Operation(
        summary = "Listar todos os hospitais",
        description = "Lista os hospitais cadastrados no sistema com paginação, ordenação e filtros opcionais " +
                      "por nome, cidade, estado e status ativo/inativo. Todos os filtros são aplicados no banco. " +
                      "Sem filtros, retorna todos os hospitais."
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> listHospitals(
            @Parameter(description = "Filtrar por nome do hospital (busca parcial)")
            @RequestParam(required = false) String name,
            
            @Parameter(description = "Filtrar por cidade")
            @RequestParam(required = false) String city,
            
            @Parameter(description = "Filtrar por estado (sigla de 2 letras)", example = "SP")
            @RequestParam(required = false) String state,
            
            @Parameter(description = "Filtrar por status ativo (true/false)")
            @RequestParam(required = false) Boolean active,
            
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals;
        
        if (name != null || city != null || state != null || active != null) {
            hospitals = hospitalService.filterHospitals(name, city, state, active, pageable);
        } else {
            hospitals = hospitalService.getAllHospitals(pageable);
        }
        
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "Visualizar informações detalhadas do hospital",
        description = "Retorna todas as informações de um hospital específico, incluindo dados de contato, " +
                      "endereço, status de ativação e estatísticas relacionadas."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalDetails(
            @Parameter(description = "ID único do hospital", example = "1", required = true)
            @PathVariable UUID id) {
        
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Resumo executivo dos hospitais",
        description = "Fornece um resumo de todos os hospitais com informações agregadas como " +
                      "número de médicos associados, status de ativação e dados essenciais para dashboard."
    )
    public ResponseEntity<ApiResponse<Page<HospitalSummary>>> getHospitalsSummary(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalSummary> summary = hospitalService.getHospitalsSummary(pageable);
        ApiResponse<Page<HospitalSummary>> response = ApiResponse.success(summary);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats")
    @Operation(
        summary = "Estatísticas dos hospitais",
        description = "Retorna o total de hospitais cadastrados e as contagens reais de hospitais ativos e inativos, " +
                      "além do instante da consulta."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHospitalStats() {
        Map<String, Object> stats = hospitalService.getHospitalStats();
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
        return ResponseEntity.ok(response);
    }

    // ======================= CADASTRO E EDIÇÃO =======================

    @PostMapping
    @Operation(
        summary = "Cadastrar novo hospital",
        description = "Cadastra um novo hospital na plataforma com todas as informações necessárias. " +
                      "Valida CNPJ único e campos obrigatórios. O hospital é criado como ativo por padrão."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.createHospital(request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital, 
            "Hospital '" + hospital.name() + "' cadastrado com sucesso");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "Editar informações do hospital",
        description = "Atualiza todas as informações de um hospital existente. " +
                      "Permite modificar dados de contato, endereço e informações corporativas. " +
                      "Valida unicidade do CNPJ se alterado."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @Parameter(description = "ID único do hospital", example = "1", required = true)
            @PathVariable UUID id,
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.updateHospital(id, request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital,
            "Hospital '" + hospital.name() + "' atualizado com sucesso");
        
        return ResponseEntity.ok(response);
    }

    // ======================= ATIVAÇÃO E INATIVAÇÃO =======================

    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Ativar hospital",
        description = "Ativa um hospital, habilitando o acesso de seus médicos à plataforma. " +
                      "Médicos de hospitais ativos podem fazer login e acessar funcionalidades do sistema."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> activateHospital(
            @Parameter(description = "ID único do hospital", example = "1", required = true)
            @PathVariable UUID id) {
        
        hospitalService.enableHospital(id);
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital,
            "Hospital '" + hospital.name() + "' ativado com sucesso. Médicos podem fazer login.");
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Inativar hospital",
        description = "Inativa um hospital, impedindo o acesso de seus médicos à plataforma. " +
                      "Médicos de hospitais inativos não podem fazer login no sistema. " +
                      "Use esta função para suspender temporariamente o acesso."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> deactivateHospital(
            @Parameter(description = "ID único do hospital", example = "1", required = true)
            @PathVariable UUID id) {
        
        hospitalService.disableHospital(id);
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital,
            "Hospital '" + hospital.name() + "' inativado com sucesso. Acesso de médicos suspenso.");
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-status")
    @Operation(
        summary = "Alternar status do hospital",
        description = "Alterna o status de ativo/inativo do hospital. " +
                      "Se estiver ativo, torna inativo. Se estiver inativo, torna ativo."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> toggleHospitalStatus(
            @Parameter(description = "ID único do hospital", example = "1", required = true)
            @PathVariable UUID id) {
        
        HospitalResponse currentHospital = hospitalService.getHospitalById(id);
        
        if (currentHospital.active()) {
            hospitalService.disableHospital(id);
        } else {
            hospitalService.enableHospital(id);
        }
        
        HospitalResponse updatedHospital = hospitalService.getHospitalById(id);
        String action = updatedHospital.active() ? "ativado" : "inativado";
        
        ApiResponse<HospitalResponse> response = ApiResponse.success(updatedHospital,
            "Status do hospital '" + updatedHospital.name() + "' alterado para " + action);
        
        return ResponseEntity.ok(response);
    }

    // ======================= BUSCA E FILTROS =======================

    @GetMapping("/search")
    @Operation(
        summary = "Buscar hospitais por nome",
        description = "Realiza busca textual nos nomes dos hospitais. " +
                      "A busca é case-insensitive e permite correspondências parciais."
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> searchHospitalsByName(
            @Parameter(description = "Nome ou parte do nome do hospital", required = true)
            @RequestParam String query,
            
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.searchByName(query, pageable);
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    @Operation(
        summary = "Listar apenas hospitais ativos",
        description = "Retorna apenas os hospitais que estão ativos no sistema"
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> getActiveHospitals(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.getHospitalsByActive(true, pageable);
        
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/inactive")
    @Operation(
        summary = "Listar apenas hospitais inativos",
        description = "Retorna apenas os hospitais que estão inativos no sistema"
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> getInactiveHospitals(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.getHospitalsByActive(false, pageable);
        
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        return ResponseEntity.ok(response);
    }
}