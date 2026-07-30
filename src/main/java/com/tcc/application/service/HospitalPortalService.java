package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.dto.response.HospitalResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface HospitalPortalService {

    /** Dados do próprio hospital (perfil). */
    HospitalResponse getOwnHospital(String email);

    /** Atualiza os dados do próprio hospital. */
    HospitalResponse updateOwnHospital(String email, HospitalRequest request);

    /** Dashboard com estatísticas do próprio hospital. */
    HospitalDashboardResponse getDashboard(String email);

    /** Lista paginada de médicos do próprio hospital. */
    Page<DoctorResponse> listDoctors(String email, Pageable pageable);

    /** Cadastra um médico vinculado automaticamente ao hospital do usuário autenticado. */
    DoctorResponse createDoctor(String email, DoctorRequest request);

    /** Busca um médico por ID, validando que pertence ao hospital do usuário autenticado. */
    DoctorResponse getDoctorById(String email, UUID doctorId);

    /** Atualiza um médico, validando que pertence ao hospital do usuário autenticado. */
    DoctorResponse updateDoctor(String email, UUID doctorId, DoctorRequest request);

    /** Remove um médico, validando que pertence ao hospital do usuário autenticado. */
    void deleteDoctor(String email, UUID doctorId);
}
