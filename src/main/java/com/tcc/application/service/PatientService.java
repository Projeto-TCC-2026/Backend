package com.tcc.application.service;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;

import java.util.List;

public interface PatientService {
    
    PatientResponse createPatient(PatientRequest request);
    
    List<PatientResponse> getAllActivePatients();
    
    PatientResponse getPatientById(Long id);
    
    PatientResponse updatePatient(Long id, PatientRequest request);
    
    void inactivatePatient(Long id);
}