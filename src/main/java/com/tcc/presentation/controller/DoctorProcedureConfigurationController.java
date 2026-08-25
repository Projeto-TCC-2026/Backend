package com.tcc.presentation.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.request.DoctorProcedureFieldRequest;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorProcedureFieldResponse;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.FieldTypePresetResponse;
import com.tcc.application.service.DoctorProcedureConfigurationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/doctor/my-procedures")
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorProcedureConfigurationController {
  private final DoctorProcedureConfigurationService service;

  public DoctorProcedureConfigurationController(DoctorProcedureConfigurationService service) {
    this.service = service;
  }

  @GetMapping("/field-type-presets")
  public ResponseEntity<ApiResponse<List<FieldTypePresetResponse>>> presets() {
    return ResponseEntity.ok(ApiResponse.success(service.listFieldTypePresets()));
  }

  @GetMapping
  public ResponseEntity<ApiResponse<List<DoctorProcedureResponse>>> list(Authentication authentication) {
    return ResponseEntity.ok(ApiResponse.success(service.listOwnProcedures(email(authentication))));
  }

  @GetMapping("/{doctorProcedureId}/fields")
  public ResponseEntity<ApiResponse<List<DoctorProcedureFieldResponse>>> fields(
      Authentication authentication, @PathVariable UUID doctorProcedureId) {
    return ResponseEntity.ok(ApiResponse.success(service.listFields(email(authentication), doctorProcedureId)));
  }

  @PutMapping("/{doctorProcedureId}/fields")
  public ResponseEntity<ApiResponse<List<DoctorProcedureFieldResponse>>> replaceFields(
      Authentication authentication,
      @PathVariable UUID doctorProcedureId,
      @Valid @RequestBody List<DoctorProcedureFieldRequest> requests) {
    return ResponseEntity.ok(ApiResponse.success(
        service.replaceFields(email(authentication), doctorProcedureId, requests)));
  }

  private String email(Authentication authentication) {
    return ((UserDetails) authentication.getPrincipal()).getUsername();
  }
}
