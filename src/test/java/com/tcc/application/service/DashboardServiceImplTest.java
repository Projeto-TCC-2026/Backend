package com.tcc.application.service;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.infrastructure.storage.JsonCacheStorage;

@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    private static final String CACHE_KEY = "dashboard/admin.json";

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private ProcedureExecutionRepository procedureExecutionRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private JsonCacheStorage jsonCacheStorage;

    private DashboardServiceImpl dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardServiceImpl(
                hospitalRepository,
                doctorRepository,
                patientRepository,
                procedureRepository,
                procedureExecutionRepository,
                alertRepository,
                userRepository,
                patientMapper,
                jsonCacheStorage,
                new ObjectMapper(),
                CACHE_KEY);
    }

    @Nested
    @DisplayName("getAdminDashboardCached")
    class GetAdminDashboardCached {

        @Test
        @DisplayName("cache existente é devolvido sem consultar o banco")
        void shouldReturnCachedValueWithoutHittingDatabase() {
            String json = """
                    {"totalHospitals":10,"activeHospitals":8,"inactiveHospitals":2,\
                    "totalDoctors":30,"activeDoctors":25,"inactiveDoctors":5,\
                    "totalPatients":400}""";

            when(jsonCacheStorage.read(CACHE_KEY)).thenReturn(Optional.of(json));

            AdminDashboardResponse result = dashboardService.getAdminDashboardCached();

            assertThat(result).isEqualTo(new AdminDashboardResponse(
                    10L, 8L, 2L, 30L, 25L, 5L, 400L));

            verifyNoInteractions(
                    hospitalRepository,
                    doctorRepository,
                    patientRepository);
        }

        @Test
        @DisplayName("cache vazio cai no cálculo do banco")
        void shouldFallBackToDatabaseWhenCacheIsEmpty() {
            when(jsonCacheStorage.read(CACHE_KEY)).thenReturn(Optional.empty());

            when(hospitalRepository.countTotalHospitals()).thenReturn(3L);
            when(hospitalRepository.countByActiveTrue()).thenReturn(2L);
            when(hospitalRepository.countByActiveFalse()).thenReturn(1L);
            when(doctorRepository.countTotalDoctors()).thenReturn(7L);
            when(doctorRepository.countByActiveTrueTotal()).thenReturn(5L);
            when(patientRepository.count()).thenReturn(42L);

            AdminDashboardResponse result = dashboardService.getAdminDashboardCached();

            assertThat(result).isEqualTo(new AdminDashboardResponse(
                    3L, 2L, 1L, 7L, 5L, 2L, 42L));
        }
    }
}
