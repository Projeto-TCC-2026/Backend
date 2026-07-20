package com.tcc.application.service;

import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportService {
    
    List<PatientsByHospitalResponse> getPatientsByHospital();
    
    List<DoctorsByHospitalResponse> getDoctorsByHospital();
    
    List<ProceduresByDoctorResponse> getProceduresByDoctor();
    
    List<ProceduresByPeriodResponse> getProceduresByPeriod(LocalDateTime startDate, LocalDateTime endDate);
}
