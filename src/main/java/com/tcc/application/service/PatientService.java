package com.tcc.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.request.PatientUpdateRequest;
import com.tcc.application.dto.response.AccessLinkResponse;
import com.tcc.application.dto.response.PatientRegistrationResponse;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.ProcedureExecutionResponse;

public interface PatientService {

    /**
     * Cria a conta de acesso do paciente (role PATIENT, com senha temporária desconhecida),
     * o paciente em si, e o vincula ao médico autenticado, identificado pelo e-mail do token.
     * Tudo na mesma transação: paciente cadastrado por um médico nunca fica sem médico
     * responsável, e cadastro desfeito não deixa conta órfã nem dispara e-mail.
     *
     * <p>Se o e-mail pertencer a um paciente inativo, o cadastro reativa a conta existente
     * em vez de recusar por duplicidade.
     */
    PatientRegistrationResponse createPatient(String email, PatientRequest request);

    /**
     * Reemite o link de primeiro acesso do paciente, invalidando o anterior. Restrito a
     * quem tem acesso ao paciente: o escopo é aplicado na consulta, não só pela role.
     */
    AccessLinkResponse generateAccessLink(String requesterEmail, UUID patientId);

    Page<PatientResponse> getAllActivePatients(String requesterEmail, Pageable pageable);

    PatientResponse getPatientById(String requesterEmail, UUID id);

    /**
     * Atualiza os dados cadastrais do paciente. Não mexe em procedimento: o vínculo é
     * gerenciado pelo {@link PatientProcedureService}.
     */
    PatientResponse updatePatient(String requesterEmail, UUID id, PatientUpdateRequest request);

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
