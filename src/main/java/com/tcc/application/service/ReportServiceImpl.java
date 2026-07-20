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
        // Usar agregação no banco de dados (GROUP BY)
        List<Object[]> results = patientRepository.countPatientsByHospital();
        
        return results.stream()
                .map(row -> new PatientsByHospitalResponse(
                        ((Number) row[0]).longValue(),  // hospitalId
                        (String) row[1],                // hospitalName
                        ((Number) row[2]).longValue()   // totalPatients
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorsByHospitalResponse> getDoctorsByHospital() {
        // Usar agregação no banco de dados (GROUP BY)
        List<Object[]> results = doctorRepository.countDoctorsByHospital();
        
        return results.stream()
                .map(row -> new DoctorsByHospitalResponse(
                        ((Number) row[0]).longValue(),  // hospitalId
                        (String) row[1],                // hospitalName
                        ((Number) row[2]).longValue()   // totalDoctors
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByDoctorResponse> getProceduresByDoctor() {
        // Usar agregação no banco de dados (GROUP BY)
        List<Object[]> results = procedureRepository.countProceduresByDoctor();
        
        return results.stream()
                .map(row -> new ProceduresByDoctorResponse(
                        ((Number) row[0]).longValue(),  // doctorId
                        (String) row[1],                // doctorName
                        (String) row[2],                // specialty
                        ((Number) row[3]).longValue()   // totalProcedures
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByPeriodResponse> getProceduresByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        // Validar datas
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias");
        }
        
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim");
        }
        
        // Usar agregação no banco de dados (GROUP BY com DATE_FORMAT)
        List<Object[]> results = procedureRepository.countProceduresByPeriod(startDate, endDate);
        
        return results.stream()
                .map(row -> new ProceduresByPeriodResponse(
                        (String) row[0],                // period (YYYY-MM)
                        ((Number) row[1]).longValue()   // totalProcedures
                ))
                .collect(Collectors.toList());
    }
}
