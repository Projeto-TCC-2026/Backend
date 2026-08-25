package com.tcc.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.DoctorProcedureRequest;
import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.service.ProcedureService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/procedures")
@PreAuthorize("hasRole('ADMIN')")
public class AdminProcedureController {

    private final ProcedureService procedureService;

    public AdminProcedureController(ProcedureService procedureService) {
        this.procedureService = procedureService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProcedureResponse>> create(
            @RequestParam UUID hospitalId,
            @Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(procedureService.createProcedureForHospital(hospitalId, request)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProcedureResponse>>> list(
            @RequestParam UUID hospitalId,
            @RequestParam(defaultValue = "false") Boolean includeInactive,
            @PageableDefault(size = 10, sort = "title", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(
                procedureService.listProceduresForHospital(hospitalId, includeInactive, pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProcedureResponse>> get(
            @RequestParam UUID hospitalId, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(procedureService.getProcedureByIdForHospital(hospitalId, id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProcedureResponse>> update(
            @RequestParam UUID hospitalId, @PathVariable UUID id,
            @Valid @RequestBody ProcedureRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                procedureService.updateProcedureForHospital(hospitalId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @RequestParam UUID hospitalId, @PathVariable UUID id) {
        procedureService.deactivateProcedureForHospital(hospitalId, id);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @GetMapping("/{id}/doctors")
    public ResponseEntity<ApiResponse<List<DoctorProcedureResponse>>> listDoctors(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(procedureService.listProcedureDoctorsForAdmin(id)));
    }

    @PostMapping("/{id}/doctors")
    public ResponseEntity<ApiResponse<DoctorProcedureResponse>> assignDoctor(
            @PathVariable UUID id, @Valid @RequestBody DoctorProcedureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(procedureService.assignDoctorForAdmin(id, request)));
    }

    @DeleteMapping("/{id}/doctors/{doctorId}")
    public ResponseEntity<ApiResponse<Void>> unassignDoctor(
            @PathVariable UUID id, @PathVariable UUID doctorId) {
        procedureService.unassignDoctorForAdmin(id, doctorId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}