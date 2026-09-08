package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Patient;

/**
 * Fluxo do médico: consultar o próprio catálogo autorizado pelo hospital e
 * atribuir procedimentos aos pacientes vinculados a ele.
 * O médico é sempre derivado do e-mail do usuário autenticado.
 */
public interface PatientProcedureService {

    List<ProcedureResponse> listAvailableProcedures(String email);

    PatientProcedureResponse assignProcedure(String email, UUID patientId, PatientProcedureRequest request);

    /**
     * Cria os vínculos iniciais durante o cadastro do paciente, garantindo que nenhum
     * paciente exista sem procedimento ativo.
     *
     * <p>Recebe {@code doctor} e {@code patient} já em memória de propósito: no cadastro,
     * o paciente e o {@code DoctorPatient} acabaram de ser criados na mesma transação e
     * ainda não estão visíveis para consulta. Reconferir o vínculo pelo repository
     * dependeria de flush implícito e refaria consultas que o chamador já resolveu.
     * A autorização do procedimento ao médico continua sendo validada aqui.
     */
    List<PatientProcedureResponse> assignInitialProcedures(Doctor doctor, Patient patient,
                                                          List<PatientProcedureRequest> requests);

    Page<PatientProcedureResponse> listPatientProcedures(String email, UUID patientId, Pageable pageable);

    PatientProcedureResponse updateAssignment(String email, UUID patientId, UUID assignmentId,
                                              PatientProcedureRequest request);

    void removeAssignment(String email, UUID patientId, UUID assignmentId);
}
