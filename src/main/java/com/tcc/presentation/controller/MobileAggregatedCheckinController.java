package com.tcc.presentation.controller;

import com.tcc.application.dto.request.AggregatedManualCheckinRequest;
import com.tcc.application.dto.response.AggregatedCheckinResponse;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.service.CheckinService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/checkins")
@PreAuthorize("hasRole('PATIENT')")
public class MobileAggregatedCheckinController {

    private final CheckinService checkinService;

    public MobileAggregatedCheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AggregatedCheckinResponse>> submit(
            Authentication authentication,
            @Valid @RequestBody AggregatedManualCheckinRequest request) {
        String email = ((UserDetails) authentication.getPrincipal()).getUsername();
        AggregatedCheckinResponse response = checkinService.submitAggregatedManual(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
