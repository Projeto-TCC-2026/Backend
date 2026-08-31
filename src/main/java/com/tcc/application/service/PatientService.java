package com.tcc.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.ProcedureExecutionResponse;

public interface PatientService {

    /**
     * Cria o paciente e o vincula ao médico autenticado, identificado pelo e-mail do token.
     * O vínculo é criado na mesma transação: paciente cadastrado por um médico nunca fica
     * sem médico responsável.
     */
    PatientResponse createPatient(String email, PatientRequest request);

    Page<PatientResponse> getAllActivePatients(String requesterEmail, Pageable pageable);

    PatientResponse getPatientById(String requesterEmail, UUID id);

    PatientResponse updatePatient(String requesterEmail, UUID id, PatientRequest request);

    void deletePatient(UUID id);

    void inactivatePatient(String requesterEmail, UUID id);

    Page<PatientResponse> searchByName(String requesterEmail, String name, Pageable pageable);

    Page<PatientResponse> searchByCpf(String requesterEmail, String cpf, Pageable pageable);

    Page<PatientResponse> searchByEmail(String requesterEmail, String email, Pageable pageable);

    Page<PatientResponse> searchByPhone(String requesterEmail, String phone, Pageable pageable);

    Page<PatientResponse> filterPatients(String requesterEmail, String name, String gender, String city, String state, Pageable pageable);

    Page<ProcedureExecutionResponse> getPatientProcedureExecutions(String requesterEmail, UUID patientId, Pageable pageable);

    Long countPatientProcedureExecutions(String requesterEmail, UUID patientId);

    long countAllPatients();

    long countActivePatients();
}
