package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DoctorService {
    
    DoctorResponse createDoctor(DoctorRequest request);
    
    Page<DoctorResponse> getAllDoctors(Pageable pageable);
    
    DoctorResponse getDoctorById(UUID id);
    
    DoctorResponse updateDoctor(UUID id, DoctorRequest request);
    
    void deleteDoctor(UUID id);
    
    Page<DoctorResponse> searchByName(String name, Pageable pageable);
    
    DoctorResponse searchByCrm(String crm);
    
    Page<DoctorResponse> searchBySpecialty(String specialty, Pageable pageable);
    
    Page<DoctorResponse> filterDoctors(UUID hospitalId, String specialty, String name, String crm, Pageable pageable);
}
