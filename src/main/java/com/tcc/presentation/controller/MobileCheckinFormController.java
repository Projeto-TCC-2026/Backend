package com.tcc.presentation.controller;

import java.util.UUID;

import com.tcc.application.dto.request.ManualCheckinRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.CheckinFormResponse;
import com.tcc.application.dto.response.CheckinResponse;
import com.tcc.application.service.CheckinService;
import com.tcc.application.service.DoctorProcedureConfigurationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@RestController
@RequestMapping("/api/mobile/patient-procedures")
@PreAuthorize("hasRole('PATIENT')")
public class MobileCheckinFormController {
  private final DoctorProcedureConfigurationService service;
  private final CheckinService checkinService;

  public MobileCheckinFormController(DoctorProcedureConfigurationService service, CheckinService checkinService) {
    this.service = service;
    this.checkinService = checkinService;
  }

  @GetMapping("/{patientProcedureId}/checkin-form")
  public ResponseEntity<ApiResponse<CheckinFormResponse>> getForm(
      Authentication authentication, @PathVariable UUID patientProcedureId) {
    String email = ((UserDetails) authentication.getPrincipal()).getUsername();
    return ResponseEntity.ok(ApiResponse.success(service.getPatientCheckinForm(email, patientProcedureId)));
  }

  @PostMapping("/{patientProcedureId}/checkins")
  public ResponseEntity<ApiResponse<CheckinResponse>> submitManualCheckin(
      Authentication authentication,
      @PathVariable UUID patientProcedureId,
      @Valid @RequestBody ManualCheckinRequest request) {
    String email = ((UserDetails) authentication.getPrincipal()).getUsername();
    CheckinResponse checkin = checkinService.submitManual(email, patientProcedureId, request);
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(checkin));
  }
}
