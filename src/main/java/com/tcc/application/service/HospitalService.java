package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.HospitalSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HospitalService {
    
    HospitalResponse createHospital(HospitalRequest request);
    
    Page<HospitalResponse> getAllHospitals(Pageable pageable);
    
    HospitalResponse getHospitalById(UUID id);
    
    HospitalResponse updateHospital(UUID id, HospitalRequest request);
    
    void deleteHospital(UUID id);
    
    Page<HospitalResponse> searchByName(String name, Pageable pageable);
    
    Page<HospitalResponse> filterHospitals(String name, String city, String state, Pageable pageable);
    
    long countHospitals();
    
    long countActiveHospitals();
    
    long countInactiveHospitals();
    
    void enableHospital(UUID id);
    
    void disableHospital(UUID id);
    
    Page<HospitalSummary> getHospitalsSummary(Pageable pageable);
}
