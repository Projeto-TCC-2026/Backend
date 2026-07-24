package com.tcc.application.service;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.dto.response.PatientSummary;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.UnauthorizedException;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ProcedureRepository procedureRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;

    public DashboardServiceImpl(HospitalRepository hospitalRepository,
                                DoctorRepository doctorRepository,
                                PatientRepository patientRepository,
                                ProcedureRepository procedureRepository,
                                UserRepository userRepository,
                                PatientMapper patientMapper) {
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
        this.userRepository = userRepository;
        this.patientMapper = patientMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        // Usar COUNT diretamente no banco para melhor performance
        Long totalHospitals = hospitalRepository.countTotalHospitals();
        Long totalDoctors = doctorRepository.countTotalDoctors();
        Long totalPatients = patientRepository.countActivePatientsTotal();
        Long totalProcedures = procedureRepository.countActiveProceduresTotal();
        
        // Como Hospital não tem campo active, todos são considerados ativos
        // Se precisar adicionar lógica de ativo/inativo, adicione o campo na entidade Hospital
        Long activeHospitals = totalHospitals;
        Long inactiveHospitals = 0L;

        return new AdminDashboardResponse(
                totalHospitals,
                totalDoctors,
                totalPatients,
                totalProcedures,
                activeHospitals,
                inactiveHospitals
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalDashboardResponse getHospitalDashboard(Long hospitalId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        if (requester.getRole() == Role.DOCTOR) {
            Long requesterHospitalId = doctorRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new UnauthorizedException("Perfil de médico não encontrado"))
                    .getHospital().getId();

            if (!hospitalId.equals(requesterHospitalId)) {
                throw new UnauthorizedException("Você não tem permissão para acessar dados deste hospital");
            }
        }

        // Verificar se o hospital existe
        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + hospitalId));

        // Usar COUNT diretamente no banco
        Long totalDoctors = doctorRepository.countByHospitalId(hospitalId);
        Long totalPatients = patientRepository.countActivePatientsByHospitalId(hospitalId);
        Long totalProcedures = procedureRepository.countActiveProceduresByHospitalId(hospitalId);

        // Buscar procedimentos por período (últimos 12 meses)
        LocalDateTime endDate = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(12);
        
        List<Object[]> proceduresData = procedureRepository.countProceduresByPeriodAndHospitalId(
                hospitalId, startDate, endDate);
        
        List<ProceduresByPeriodResponse> proceduresByPeriod = proceduresData.stream()
                .map(row -> new ProceduresByPeriodResponse(
                        formatPeriod((Number) row[0], (Number) row[1]),
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());

        // Buscar últimos 10 pacientes
        List<Patient> latestPatientsEntities = patientRepository.findLatestPatientsByHospitalId(
                hospitalId, PageRequest.of(0, 10));
        
        List<PatientSummary> latestPatients = latestPatientsEntities.stream()
                .map(patientMapper::toSummary)
                .collect(Collectors.toList());

        return new HospitalDashboardResponse(
                hospital.getId(),
                hospital.getName(),
                totalDoctors,
                totalPatients,
                totalProcedures,
                proceduresByPeriod,
                latestPatients
        );
    }

    private String formatPeriod(Number year, Number month) {
        return "%04d-%02d".formatted(year.intValue(), month.intValue());
    }
}
