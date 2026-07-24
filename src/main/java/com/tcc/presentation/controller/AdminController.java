package com.tcc.presentation.controller;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.request.UserRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.UserResponse;
import com.tcc.application.dto.response.HospitalSummary;
import com.tcc.application.service.HospitalService;
import com.tcc.application.service.UserService;
import com.tcc.domain.model.Role;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administração", description = "Módulo administrativo para gerenciamento da plataforma - Acesso exclusivo ADMIN")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final HospitalService hospitalService;
    private final UserService userService;

    public AdminController(HospitalService hospitalService, UserService userService) {
        this.hospitalService = hospitalService;
        this.userService = userService;
    }

    // ======================= DASHBOARD ADMINISTRATIVO =======================

    @GetMapping("/dashboard")
    @Operation(
        summary = "Dashboard administrativo",
        description = "Retorna estatísticas gerais da plataforma para o painel administrativo"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        // Implementar estatísticas do sistema
        Map<String, Object> dashboard = Map.of(
            "totalHospitals", hospitalService.countHospitals(),
            "totalUsers", userService.countUsers(),
            "totalDoctors", userService.countUsersByRole(Role.DOCTOR),
            "totalPatients", userService.countUsersByRole(Role.PATIENT),
            "message", "Bem-vindo ao painel administrativo"
        );
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(dashboard);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Perfil do administrador logado",
        description = "Retorna informações do administrador atualmente logado"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getAdminProfile(Authentication authentication) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        UserResponse admin = userService.getUserByEmail(userDetails.getUsername());
        
        ApiResponse<UserResponse> response = ApiResponse.success(admin);
        return ResponseEntity.ok(response);
    }

    // ======================= GERENCIAMENTO DE HOSPITAIS =======================

    @PostMapping("/hospitals")
    @Operation(
        summary = "Cadastrar hospital (Admin)",
        description = "Cadastra um novo hospital na plataforma. Apenas administradores podem executar esta ação."
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> createHospital(
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.createHospital(request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital, "Hospital cadastrado com sucesso");
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/hospitals")
    @Operation(
        summary = "Listar todos os hospitais (Admin)",
        description = "Lista todos os hospitais cadastrados na plataforma com paginação e ordenação"
    )
    public ResponseEntity<ApiResponse<Page<HospitalResponse>>> getAllHospitals(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalResponse> hospitals = hospitalService.getAllHospitals(pageable);
        ApiResponse<Page<HospitalResponse>> response = ApiResponse.success(hospitals);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hospitals/{id}")
    @Operation(
        summary = "Visualizar hospital específico (Admin)",
        description = "Retorna os detalhes completos de um hospital específico"
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> getHospitalById(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        HospitalResponse hospital = hospitalService.getHospitalById(id);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/hospitals/{id}")
    @Operation(
        summary = "Atualizar hospital (Admin)",
        description = "Atualiza as informações de um hospital existente"
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> updateHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody HospitalRequest request) {
        
        HospitalResponse hospital = hospitalService.updateHospital(id, request);
        ApiResponse<HospitalResponse> response = ApiResponse.success(hospital, "Hospital atualizado com sucesso");
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/hospitals/{id}")
    @Operation(
        summary = "Excluir hospital (Admin)",
        description = "Remove um hospital da plataforma. Verifica se não há doutores vinculados antes da exclusão."
    )
    public ResponseEntity<ApiResponse<Void>> deleteHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        hospitalService.deleteHospital(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/hospitals/{id}/enable")
    @Operation(
        summary = "Habilitar acesso do hospital",
        description = "Habilita o acesso de um hospital na plataforma, permitindo que seus médicos façam login"
    )
    public ResponseEntity<ApiResponse<Void>> enableHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        hospitalService.enableHospital(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/hospitals/{id}/disable")
    @Operation(
        summary = "Desabilitar acesso do hospital",
        description = "Desabilita o acesso de um hospital na plataforma, impedindo que seus médicos façam login"
    )
    public ResponseEntity<ApiResponse<Void>> disableHospital(
            @Parameter(description = "ID do hospital", example = "1", required = true)
            @PathVariable Long id) {
        
        hospitalService.disableHospital(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/hospitals/summary")
    @Operation(
        summary = "Resumo dos hospitais",
        description = "Retorna um resumo dos hospitais cadastrados com estatísticas básicas"
    )
    public ResponseEntity<ApiResponse<Page<HospitalSummary>>> getHospitalsSummary(
            @PageableDefault(size = 10, sort = "name", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<HospitalSummary> summary = hospitalService.getHospitalsSummary(pageable);
        ApiResponse<Page<HospitalSummary>> response = ApiResponse.success(summary);
        
        return ResponseEntity.ok(response);
    }

    // ======================= GERENCIAMENTO DE USUÁRIOS =======================

    @PostMapping("/users")
    @Operation(
        summary = "Cadastrar usuário (Admin)",
        description = "Cadastra um novo usuário no sistema com role específica (ADMIN, DOCTOR, PATIENT)"
    )
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody UserRequest request) {
        
        UserResponse user = userService.createUser(request);
        ApiResponse<UserResponse> response = ApiResponse.success(user);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/users")
    @Operation(
        summary = "Listar todos os usuários (Admin)",
        description = "Lista todos os usuários cadastrados no sistema com paginação e filtros por role"
    )
    public ResponseEntity<ApiResponse<Page<UserResponse>>> getAllUsers(
            @Parameter(description = "Filtrar por role (ADMIN, DOCTOR, PATIENT)")
            @RequestParam(required = false) Role role,
            @PageableDefault(size = 10, sort = "email", direction = Sort.Direction.ASC) Pageable pageable) {
        
        Page<UserResponse> users = userService.getAllUsers(role, pageable);
        ApiResponse<Page<UserResponse>> response = ApiResponse.success(users);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/users/{id}")
    @Operation(
        summary = "Visualizar usuário específico (Admin)",
        description = "Retorna os detalhes de um usuário específico"
    )
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {
        
        UserResponse user = userService.getUserById(id);
        ApiResponse<UserResponse> response = ApiResponse.success(user);
        
        return ResponseEntity.ok(response);
    }

    @PutMapping("/users/{id}")
    @Operation(
        summary = "Atualizar usuário (Admin)",
        description = "Atualiza as informações de um usuário existente"
    )
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request) {
        
        UserResponse user = userService.updateUser(id, request);
        ApiResponse<UserResponse> response = ApiResponse.success(user);
        
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/users/{id}")
    @Operation(
        summary = "Excluir usuário (Admin)",
        description = "Remove um usuário do sistema (soft delete)"
    )
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {
        
        userService.deleteUser(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/activate")
    @Operation(
        summary = "Ativar usuário",
        description = "Reativa um usuário previamente desativado"
    )
    public ResponseEntity<ApiResponse<Void>> activateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {
        
        userService.activateUser(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/users/{id}/deactivate")
    @Operation(
        summary = "Desativar usuário",
        description = "Desativa um usuário, impedindo seu login no sistema"
    )
    public ResponseEntity<ApiResponse<Void>> deactivateUser(
            @Parameter(description = "ID do usuário", example = "1", required = true)
            @PathVariable Long id) {
        
        userService.deactivateUser(id);
        ApiResponse<Void> response = ApiResponse.success();
        
        return ResponseEntity.ok(response);
    }
}