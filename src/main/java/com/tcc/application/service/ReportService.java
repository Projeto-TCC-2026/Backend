package com.tcc.application.service;

import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientCheckinStatusResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ReportService {
    
    List<PatientsByHospitalResponse> getPatientsByHospital();
    
    List<DoctorsByHospitalResponse> getDoctorsByHospital();
    
    List<ProceduresByDoctorResponse> getProceduresByDoctor();
    
    List<ProceduresByPeriodResponse> getProceduresByPeriod(LocalDateTime startDate, LocalDateTime endDate);

    byte[] exportCheckins(String email, LocalDate startDate, LocalDate endDate, UUID procedureId,
                          UUID patientId, UUID doctorId);

    byte[] exportAlerts(String email, LocalDate startDate, LocalDate endDate, UUID procedureId,
                        UUID patientId, UUID doctorId);

    /**
     * Verifica se um paciente realizou pelo menos um check-in no dia informado.
     * Aplica o escopo do usuário autenticado (DOCTOR ou HOSPITAL).
     */
    PatientCheckinStatusResponse getDailyCheckinStatus(String email, LocalDate date, UUID patientId);

}
