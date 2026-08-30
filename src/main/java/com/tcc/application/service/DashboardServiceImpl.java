package com.tcc.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.DoctorDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.dto.response.PatientSummary;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import com.tcc.infrastructure.storage.JsonCacheStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(
            DashboardServiceImpl.class);

    private final HospitalRepository hospitalRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final ProcedureRepository procedureRepository;
    private final ProcedureExecutionRepository procedureExecutionRepository;
    private final AlertRepository alertRepository;
    private final UserRepository userRepository;
    private final PatientMapper patientMapper;
    private final JsonCacheStorage jsonCacheStorage;
    private final ObjectMapper objectMapper;
    private final String adminCacheKey;

    public DashboardServiceImpl(HospitalRepository hospitalRepository,
                                DoctorRepository doctorRepository,
                                PatientRepository patientRepository,
                                ProcedureRepository procedureRepository,
                                ProcedureExecutionRepository procedureExecutionRepository,
                                AlertRepository alertRepository,
                                UserRepository userRepository,
                                PatientMapper patientMapper,
                                JsonCacheStorage jsonCacheStorage,
                                ObjectMapper objectMapper,
                                @Value("${app.dashboard-cache.admin-key}") String adminCacheKey) {
        this.hospitalRepository = hospitalRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.procedureRepository = procedureRepository;
        this.procedureExecutionRepository = procedureExecutionRepository;
        this.alertRepository = alertRepository;
        this.userRepository = userRepository;
        this.patientMapper = patientMapper;
        this.jsonCacheStorage = jsonCacheStorage;
        this.objectMapper = objectMapper;
        this.adminCacheKey = adminCacheKey;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboard() {
        return calculateAdminDashboard();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponse getAdminDashboardCached() {
        Optional<String> cached = jsonCacheStorage.read(adminCacheKey);

        if (cached.isPresent()) {
            try {
                AdminDashboardResponse response = objectMapper.readValue(
                        cached.get(), AdminDashboardResponse.class);

                log.info("Dashboard admin servido do cache. key={}", adminCacheKey);
                return response;

            } catch (JsonProcessingException e) {
                log.error(
                        "Cache do dashboard admin ilegível; recalculando no banco. key={}, exception={}",
                        adminCacheKey,
                        e.getClass().getSimpleName());
            }
        } else {
            log.info(
                    "Cache do dashboard admin ausente; calculando no banco. key={}",
                    adminCacheKey);
        }

        return calculateAdminDashboard();
    }

    private AdminDashboardResponse calculateAdminDashboard() {
        Long totalHospitals    = hospitalRepository.countTotalHospitals();
        Long activeHospitals   = hospitalRepository.countByActiveTrue();
        Long inactiveHospitals = hospitalRepository.countByActiveFalse();
        Long totalDoctors      = doctorRepository.countTotalDoctors();
        Long activeDoctors     = doctorRepository.countByActiveTrueTotal();
        Long inactiveDoctors   = totalDoctors - activeDoctors;
        Long totalPatients     = (long) patientRepository.count();

        return new AdminDashboardResponse(
                totalHospitals,
                activeHospitals,
                inactiveHospitals,
                totalDoctors,
                activeDoctors,
                inactiveDoctors,
                totalPatients
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalDashboardResponse getHospitalDashboard(UUID hospitalId, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        if (requester.getRole() == Role.DOCTOR) {
            UUID requesterHospitalId = doctorRepository.findByUserId(requester.getId())
                    .orElseThrow(() -> new UnauthorizedException("Perfil de médico não encontrado"))
                    .getHospital().getId();

            if (!hospitalId.equals(requesterHospitalId)) {
                throw new UnauthorizedException("Você não tem permissão para acessar dados deste hospital");
            }
        }

        if (requester.getRole() == Role.HOSPITAL) {
            if (requester.getHospital() == null || !hospitalId.equals(requester.getHospital().getId())) {
                throw new UnauthorizedException("Você não tem permissão para acessar dados deste hospital");
            }
        }

        Hospital hospital = hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + hospitalId));

        Long totalDoctors    = doctorRepository.countByHospitalId(hospitalId);
        Long totalPatients   = patientRepository.countActivePatientsByHospitalId(hospitalId)
                             + patientRepository.countInactivePatientsByHospitalId(hospitalId);
        Long activePatients  = patientRepository.countActivePatientsByHospitalId(hospitalId);
        Long totalProcedures = procedureRepository.countActiveProceduresByHospitalId(hospitalId);
        Long pendingAlerts   = alertRepository.countPendingAlertsByHospitalId(hospitalId);

        LocalDateTime endDate   = LocalDateTime.now();
        LocalDateTime startDate = endDate.minusMonths(12);

        List<Object[]> proceduresData = procedureRepository.countProceduresByPeriodAndHospitalId(
                hospitalId, startDate, endDate);

        List<ProceduresByPeriodResponse> proceduresByPeriod = proceduresData.stream()
                .map(row -> new ProceduresByPeriodResponse(
                        formatPeriod((Number) row[0], (Number) row[1]),
                        ((Number) row[2]).longValue()
                ))
                .collect(Collectors.toList());

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
                activePatients,
                totalProcedures,
                pendingAlerts,
                proceduresByPeriod,
                latestPatients
        );
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalDashboardResponse getHospitalDashboardForDoctor(String doctorEmail) {
        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        UUID hospitalId = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Perfil de médico não encontrado"))
                .getHospital().getId();

        return getHospitalDashboard(hospitalId, doctorEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorDashboardResponse getDoctorDashboard(String doctorEmail) {
        User user = userRepository.findByEmail(doctorEmail)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        Doctor doctor = doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException("Perfil de médico não encontrado"));

        UUID doctorId = doctor.getId();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        Long totalPatients       = patientRepository.countPatientsByDoctorId(doctorId);
        Long activePatients      = patientRepository.countActivePatientsByDoctorId(doctorId);
        Long patientsWithAlert   = alertRepository.countPatientsWithPendingAlertByDoctorId(doctorId);
        Long proceduresExecuted  = procedureExecutionRepository.countByDoctorId(doctorId);
        Long newPatients         = patientRepository.countNewPatientsByDoctorId(doctorId, thirtyDaysAgo);

        return new DoctorDashboardResponse(
                doctorId,
                doctor.getFullName(),
                doctor.getHospital().getName(),
                totalPatients,
                activePatients,
                patientsWithAlert,
                proceduresExecuted,
                newPatients
        );
    }

    private String formatPeriod(Number year, Number month) {
        return "%04d-%02d".formatted(year.intValue(), month.intValue());
    }
}
