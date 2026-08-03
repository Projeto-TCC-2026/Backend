package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;

/**
 * Fluxo do médico: consultar o próprio catálogo autorizado pelo hospital e
 * atribuir procedimentos aos pacientes vinculados a ele.
 * O médico é sempre derivado do e-mail do usuário autenticado.
 */
public interface PatientProcedureService {

    List<ProcedureResponse> listAvailableProcedures(String email);

    PatientProcedureResponse assignProcedure(String email, UUID patientId, PatientProcedureRequest request);

    Page<PatientProcedureResponse> listPatientProcedures(String email, UUID patientId, Pageable pageable);

    PatientProcedureResponse updateAssignment(String email, UUID patientId, UUID assignmentId,
                                              PatientProcedureRequest request);

    void removeAssignment(String email, UUID patientId, UUID assignmentId);
}
