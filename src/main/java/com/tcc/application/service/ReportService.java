package com.tcc.application.service;

import com.tcc.application.dto.response.DoctorsByHospitalResponse;
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

}
