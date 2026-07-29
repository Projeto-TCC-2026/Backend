package com.tcc.presentation.controller;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.service.HospitalPortalService;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/hospital")
@Tag(name = "Portal do Hospital", description = "Ambiente exclusivo do hospital - acesso restrito a HOSPITAL")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('HOSPITAL')")
public class HospitalPortalController {

    private final HospitalPortalService hospitalPortalService;

    public HospitalPortalController(HospitalPortalService hospitalPortalService) {
        this.hospitalPortalService = hospitalPortalService;
    }

    @GetMapping("/profile")
    @Operation(
        summary = "Perfil do hospital",
        description = "Retorna os dados cadastrais do hospital vinculado ao usuário autenticado"
    )
    public ResponseEntity<ApiResponse<HospitalResponse>> getProfile(Authentication authentication) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(hospitalPortalService.getOwnHospital(email)));
    }

    @GetMapping("/dashboard")
    @Operation(
        summary = "Dashboard do hospital",
        description = "Retorna estatísticas do hospital: total de médicos, pacientes, procedimentos e histórico mensal"
    )
    public ResponseEntity<ApiResponse<HospitalDashboardResponse>> getDashboard(Authentication authentication) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(hospitalPortalService.getDashboard(email)));
    }

    @GetMapping("/doctors")
    @Operation(
        summary = "Listar médicos do hospital",
        description = "Retorna lista paginada dos médicos vinculados ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<Page<DoctorResponse>>> listDoctors(
            Authentication authentication,
            @PageableDefault(size = 10, sort = "fullName", direction = Sort.Direction.ASC) Pageable pageable) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(hospitalPortalService.listDoctors(email, pageable)));
    }

    @PostMapping("/doctors")
    @Operation(
        summary = "Cadastrar médico",
        description = "Cadastra um novo médico vinculado ao hospital do usuário autenticado. " +
                      "O hospitalId no corpo deve corresponder ao hospital do usuário autenticado."
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> createDoctor(
            Authentication authentication,
            @Valid @RequestBody DoctorRequest request) {
        String email = extractEmail(authentication);
        DoctorResponse doctor = hospitalPortalService.createDoctor(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(doctor));
    }

    @GetMapping("/doctors/{id}")
    @Operation(
        summary = "Buscar médico por ID",
        description = "Retorna os dados de um médico, validando que pertence ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> getDoctorById(
            Authentication authentication,
            @Parameter(description = "ID do médico", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(hospitalPortalService.getDoctorById(email, id)));
    }

    @PutMapping("/doctors/{id}")
    @Operation(
        summary = "Atualizar médico",
        description = "Atualiza os dados de um médico, validando que pertence ao hospital do usuário autenticado"
    )
    public ResponseEntity<ApiResponse<DoctorResponse>> updateDoctor(
            Authentication authentication,
            @Parameter(description = "ID do médico", required = true) @PathVariable UUID id,
            @Valid @RequestBody DoctorRequest request) {
        String email = extractEmail(authentication);
        return ResponseEntity.ok(ApiResponse.success(hospitalPortalService.updateDoctor(email, id, request)));
    }

    @DeleteMapping("/doctors/{id}")
    @Operation(
        summary = "Remover médico",
        description = "Remove um médico, validando que pertence ao hospital do usuário autenticado. " +
                      "Bloqueado se houver pacientes ou procedimentos associados."
    )
    public ResponseEntity<ApiResponse<Void>> deleteDoctor(
            Authentication authentication,
            @Parameter(description = "ID do médico", required = true) @PathVariable UUID id) {
        String email = extractEmail(authentication);
        hospitalPortalService.deleteDoctor(email, id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    // --- Helper ---

    private String extractEmail(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
