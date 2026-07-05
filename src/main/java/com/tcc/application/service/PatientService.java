package com.tcc.application.service;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PatientService {
    
    PatientResponse createPatient(PatientRequest request);
    
    Page<PatientResponse> getAllActivePatients(Pageable pageable);
    
    PatientResponse getPatientById(Long id);
    
    PatientResponse updatePatient(Long id, PatientRequest request);
    
    void deletePatient(Long id);
    
    void inactivatePatient(Long id);
    
    // Buscas adicionais
    Page<PatientResponse> searchByName(String name, Pageable pageable);
    
    Page<PatientResponse> searchByCpf(String cpf, Pageable pageable);
}