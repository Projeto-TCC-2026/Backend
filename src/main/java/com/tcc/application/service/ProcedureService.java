package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.DoctorProcedureRequest;
import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;

/**
 * Catálogo de procedimentos do hospital. Todas as operações são escopadas
 * pelo hospital do usuário autenticado, identificado pelo e-mail do token.
 */
public interface ProcedureService {

    ProcedureResponse createProcedure(String email, ProcedureRequest request);

    Page<ProcedureResponse> listProcedures(String email, Boolean includeInactive, Pageable pageable);

    ProcedureResponse getProcedureById(String email, UUID id);

    ProcedureResponse updateProcedure(String email, UUID id, ProcedureRequest request);

    void deactivateProcedure(String email, UUID id);

    List<DoctorProcedureResponse> listProcedureDoctors(String email, UUID procedureId);

    DoctorProcedureResponse assignDoctor(String email, UUID procedureId, DoctorProcedureRequest request);

    void unassignDoctor(String email, UUID procedureId, UUID doctorId);
}
