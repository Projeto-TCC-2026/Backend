package com.tcc.presentation.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.CheckinFormResponse;
import com.tcc.application.service.DoctorProcedureConfigurationService;

@RestController
@RequestMapping("/api/mobile/patient-procedures")
@PreAuthorize("hasRole('PATIENT')")
public class MobileCheckinFormController {
  private final DoctorProcedureConfigurationService service;

  public MobileCheckinFormController(DoctorProcedureConfigurationService service) {
    this.service = service;
  }

  @GetMapping("/{patientProcedureId}/checkin-form")
  public ResponseEntity<ApiResponse<CheckinFormResponse>> getForm(
      Authentication authentication, @PathVariable UUID patientProcedureId) {
    String email = ((UserDetails) authentication.getPrincipal()).getUsername();
    return ResponseEntity.ok(ApiResponse.success(service.getPatientCheckinForm(email, patientProcedureId)));
  }
}
