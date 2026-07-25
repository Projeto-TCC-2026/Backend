package com.tcc.application.service;

import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportServiceImpl implements ReportService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ProcedureRepository procedureRepository;

    public ReportServiceImpl(PatientRepository patientRepository,
                             DoctorRepository doctorRepository,
                             ProcedureRepository procedureRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.procedureRepository = procedureRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientsByHospitalResponse> getPatientsByHospital() {
        List<Object[]> results = patientRepository.countPatientsByHospital();
        
        return results.stream()
                .map(row -> new PatientsByHospitalResponse(
                        (UUID) row[0],                  // hospitalId
                        (String) row[1],                // hospitalName
                        ((Number) row[2]).longValue()   // totalPatients
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorsByHospitalResponse> getDoctorsByHospital() {
        List<Object[]> results = doctorRepository.countDoctorsByHospital();
        
        return results.stream()
                .map(row -> new DoctorsByHospitalResponse(
                        (UUID) row[0],                  // hospitalId
                        (String) row[1],                // hospitalName
                        ((Number) row[2]).longValue()   // totalDoctors
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByDoctorResponse> getProceduresByDoctor() {
        List<Object[]> results = procedureRepository.countProceduresByDoctor();
        
        return results.stream()
                .map(row -> new ProceduresByDoctorResponse(
                        (UUID) row[0],                  // doctorId
                        (String) row[1],                // doctorName
                        (String) row[2],                // specialty
                        ((Number) row[3]).longValue()   // totalProcedures
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByPeriodResponse> getProceduresByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim");
        }
        
        List<Object[]> results = procedureRepository.countProceduresByPeriod(startDate, endDate);
        
        return results.stream()
                .map(row -> new ProceduresByPeriodResponse(
                        formatPeriod((Number) row[0], (Number) row[1]),
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());
    }

    private String formatPeriod(Number year, Number month) {
        return "%04d-%02d".formatted(year.intValue(), month.intValue());
    }
}
