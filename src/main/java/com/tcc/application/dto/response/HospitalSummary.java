package com.tcc.application.dto.response;

public record HospitalSummary(
        Long id,
        String name,
        String city,
        String state
) {}
