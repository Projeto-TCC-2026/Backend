package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.HospitalSummary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface HospitalService {
    
    HospitalResponse createHospital(HospitalRequest request);
    
    Page<HospitalResponse> getAllHospitals(Pageable pageable);
    
    HospitalResponse getHospitalById(Long id);
    
    HospitalResponse updateHospital(Long id, HospitalRequest request);
    
    void deleteHospital(Long id);
    
    Page<HospitalResponse> searchByName(String name, Pageable pageable);
    
    Page<HospitalResponse> filterHospitals(String name, String city, String state, Pageable pageable);
    
    // Métodos administrativos
    long countHospitals();
    
    long countActiveHospitals();
    
    long countInactiveHospitals();
    
    void enableHospital(Long id);
    
    void disableHospital(Long id);
    
    Page<HospitalSummary> getHospitalsSummary(Pageable pageable);
}
