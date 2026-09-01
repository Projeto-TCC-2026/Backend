package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRegistrationRequest;
import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.request.UpdateDoctorProfileRequest;
import com.tcc.application.dto.response.AccessLinkResponse;
import com.tcc.application.dto.response.DoctorRegistrationResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.UserProfileResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DoctorService {
    
    DoctorResponse createDoctor(DoctorRequest request);

    /** Cria a conta de usuário (com senha temporária) e o doutor, disparando o e-mail de boas-vindas. */
    DoctorRegistrationResponse registerDoctor(DoctorRegistrationRequest request);

    /** Gera (e invalida o anterior) um novo link de ativação/primeiro acesso e reenvia o e-mail de boas-vindas. */
    AccessLinkResponse generateAccessLink(UUID doctorId);
    
    Page<DoctorResponse> getAllDoctors(Pageable pageable);
    
    DoctorResponse getDoctorById(UUID id);
    
    DoctorResponse updateDoctor(UUID id, DoctorRequest request);
    
    void deleteDoctor(UUID id);
    
    Page<DoctorResponse> searchByName(String name, Pageable pageable);
    
    DoctorResponse searchByCrm(String crm);
    
    Page<DoctorResponse> searchBySpecialty(String specialty, Pageable pageable);
    
    Page<DoctorResponse> filterDoctors(UUID hospitalId, String specialty, String name, String crm, Pageable pageable);

    UserProfileResponse updateOwnProfile(String email, UpdateDoctorProfileRequest request);
}
