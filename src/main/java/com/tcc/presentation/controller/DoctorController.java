package com.tcc.presentation.controller;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.DoctorSummary;
import com.tcc.application.service.DoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/doctors")
@Tag(name = "Doctors", description = "Endpoints para gerenciamento de médicos")
public class DoctorController {

    private final DoctorService doctorService;

    public DoctorController(DoctorService doctorService) {
        this.doctorService = doctorService;
    }

    @PostMapping
    @Operation(summary = "Criar novo médico", description = "Cria um novo médico no sistema")
    public ResponseEntity<ApiResponse<DoctorResponse>> create(@Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.create(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Listar todos os médicos", description = "Retorna uma lista resumida de todos os médicos")
    public ResponseEntity<ApiResponse<List<DoctorSummary>>> findAll() {
        List<DoctorSummary> doctors = doctorService.findAll();
        return ResponseEntity.ok(ApiResponse.success(doctors));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar médico por ID", description = "Retorna os detalhes completos de um médico")
    public ResponseEntity<ApiResponse<DoctorResponse>> findById(@PathVariable Long id) {
        DoctorResponse response = doctorService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar médico", description = "Atualiza os dados de um médico existente")
    public ResponseEntity<ApiResponse<DoctorResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody DoctorRequest request) {
        DoctorResponse response = doctorService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PatchMapping("/{id}/inactive")
    @Operation(
            summary = "Inativar médico",
            description = "Inativa um médico no sistema. NOTA: Funcionalidade ainda não implementada - aguardando definição do campo de status na entidade."
    )
    public ResponseEntity<Void> inactive(@PathVariable Long id) {
        doctorService.inactive(id);
        return ResponseEntity.noContent().build();
    }
}
