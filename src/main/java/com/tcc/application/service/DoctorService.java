package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DoctorService {
    
    DoctorResponse createDoctor(DoctorRequest request);
    
    Page<DoctorResponse> getAllDoctors(Pageable pageable);
    
    DoctorResponse getDoctorById(Long id);
    
    DoctorResponse updateDoctor(Long id, DoctorRequest request);
    
    void deleteDoctor(Long id);
    
    Page<DoctorResponse> searchByName(String name, Pageable pageable);
    
    DoctorResponse searchByCrm(String crm);
    
    Page<DoctorResponse> searchBySpecialty(String specialty, Pageable pageable);
    
    Page<DoctorResponse> filterDoctors(Long hospitalId, String specialty, String name, String crm, Pageable pageable);
}
