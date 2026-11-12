package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.application.dto.response.DailyMetricResponse;
import com.tcc.application.dto.response.ProcedureMetricsResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldDataType;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.CheckinSubmissionRepository;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HealthReadingRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.UnauthorizedException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PresentationMetricsServiceImplTest {

    private static final String EMAIL = "user@example.com";
    private static final LocalDate START = LocalDate.of(2026, 6, 8);
    private static final LocalDate END = START.plusDays(6);

    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorRepository doctorRepository;
    @Mock
    private CheckinSubmissionRepository checkinSubmissionRepository;
    @Mock
    private HealthReadingRepository healthReadingRepository;
    @Mock
    private AlertRepository alertRepository;
    @Mock
    private ProcedureRepository procedureRepository;
    @Mock
    private DoctorProcedureRepository doctorProcedureRepository;
    @Mock
    private DoctorProcedureFieldRepository fieldRepository;
    @Mock
    private CheckinRepository checkinRepository;

    private PresentationMetricsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PresentationMetricsServiceImpl(
                userRepository, doctorRepository, checkinSubmissionRepository, healthReadingRepository,
                alertRepository, procedureRepository, doctorProcedureRepository, fieldRepository, checkinRepository);
    }

    @Test
    void dailyCheckinsIncludesZeroDaysAndUsesTheAuthenticatedDoctorsScope() {
        User user = user(Role.DOCTOR);
        Doctor doctor = new Doctor();
        UUID doctorId = UUID.randomUUID();
        doctor.setId(doctorId);
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(user));
        when(doctorRepository.findByUserId(user.getId())).thenReturn(Optional.of(doctor));
        when(checkinSubmissionRepository.countDailySubmissions(
                START.atStartOfDay(), END.plusDays(1).atStartOfDay(), doctorId, null))
                .thenReturn(List.<Object[]>of(new Object[] {
                        START.getYear(), START.getMonthValue(), START.plusDays(1).getDayOfMonth(), 4L }));

        List<DailyMetricResponse> result = service.getDailyCheckinSubmissions(EMAIL, START, END);

        assertThat(result).hasSize(7);
        assertThat(result.get(0).count()).isZero();
        assertThat(result.get(1).count()).isEqualTo(4L);
        assertThat(result.get(6).date()).isEqualTo(END);
    }

    @Test
    void procedureMetricsBuildsFourteenDailyPointsAndAveragesOnlyNumericFields() {
        UUID procedureId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        User admin = user(Role.ADMIN);
        DoctorProcedureField numericField = field(fieldId, FieldDataType.DECIMAL);
        DoctorProcedureField textField = field(UUID.randomUUID(), FieldDataType.TEXT);
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(admin));
        when(procedureRepository.existsById(procedureId)).thenReturn(true);
        when(fieldRepository.findMetricsFields(procedureId, null, null))
                .thenReturn(List.of(numericField, textField));
        when(checkinRepository.findNumericFieldValues(
                List.of(fieldId), List.of(FieldDataType.INTEGER, FieldDataType.DECIMAL, FieldDataType.SCALE),
                START.minusDays(7).atStartOfDay(), END.plusDays(1).atStartOfDay(), null, null))
                .thenReturn(List.<Object[]>of(
                        new Object[] { fieldId, START.minusDays(7).atTime(10, 0), "10.0" },
                        new Object[] { fieldId, START.minusDays(7).atTime(11, 0), "14.0" }));

        ProcedureMetricsResponse result = service.getProcedureMetrics(EMAIL, procedureId, START, END);

        assertThat(result.dataStartDate()).isEqualTo(START.minusDays(7));
        assertThat(result.dataEndDate()).isEqualTo(END);
        assertThat(result.fields()).hasSize(1);
        assertThat(result.fields().get(0).dailyAverages()).hasSize(14);
        assertThat(result.fields().get(0).dailyAverages().get(0).average())
                .isEqualByComparingTo("12.00");
        assertThat(result.fields().get(0).dailyAverages().get(0).sampleSize()).isEqualTo(2);
        assertThat(result.fields().get(0).dailyAverages().get(1).average()).isNull();
    }

    @Test
    void procedureMetricsGroupsFieldsWithTheSameNameTypeAndUnit() {
        UUID procedureId = UUID.randomUUID();
        UUID temperatureFieldId = UUID.randomUUID();
        UUID equivalentTemperatureFieldId = UUID.randomUUID();
        UUID fahrenheitFieldId = UUID.randomUUID();
        DoctorProcedureField temperature = field(temperatureFieldId, FieldDataType.DECIMAL);
        temperature.setName("Temperatura");
        temperature.setUnit("°C");
        DoctorProcedureField equivalentTemperature = field(equivalentTemperatureFieldId, FieldDataType.DECIMAL);
        equivalentTemperature.setName("Temperatura");
        equivalentTemperature.setUnit("°C");
        DoctorProcedureField fahrenheitTemperature = field(fahrenheitFieldId, FieldDataType.DECIMAL);
        fahrenheitTemperature.setName("Temperatura");
        fahrenheitTemperature.setUnit("°F");
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(user(Role.ADMIN)));
        when(procedureRepository.existsById(procedureId)).thenReturn(true);
        when(fieldRepository.findMetricsFields(procedureId, null, null))
                .thenReturn(List.of(temperature, equivalentTemperature, fahrenheitTemperature));
        when(checkinRepository.findNumericFieldValues(
                List.of(temperatureFieldId, equivalentTemperatureFieldId, fahrenheitFieldId),
                List.of(FieldDataType.INTEGER, FieldDataType.DECIMAL, FieldDataType.SCALE),
                START.minusDays(7).atStartOfDay(), END.plusDays(1).atStartOfDay(), null, null))
                .thenReturn(List.<Object[]>of(
                        new Object[] { temperatureFieldId, START.minusDays(7).atTime(10, 0), "10" },
                        new Object[] { equivalentTemperatureFieldId, START.minusDays(7).atTime(11, 0), "14" },
                        new Object[] { fahrenheitFieldId, START.minusDays(7).atTime(12, 0), "50" }));

        ProcedureMetricsResponse result = service.getProcedureMetrics(EMAIL, procedureId, START, END);

        assertThat(result.fields()).hasSize(2);
        assertThat(result.fields().get(0).sourceFieldIds())
                .containsExactly(temperatureFieldId, equivalentTemperatureFieldId);
        assertThat(result.fields().get(0).name()).isEqualTo("Temperatura");
        assertThat(result.fields().get(0).unit()).isEqualTo("°C");
        assertThat(result.fields().get(0).dailyAverages().get(0).average()).isEqualByComparingTo("12");
        assertThat(result.fields().get(0).dailyAverages().get(0).sampleSize()).isEqualTo(2);
        assertThat(result.fields().get(1).unit()).isEqualTo("°F");
        assertThat(result.fields().get(1).dailyAverages().get(0).average()).isEqualByComparingTo("50");
    }

    @Test
    void hospitalCanAccessMetricsForAProcedureOwnedByItsHospital() {
        UUID procedureId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();
        User hospitalUser = user(Role.HOSPITAL);
        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospitalUser.setHospital(hospital);
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
        when(procedureRepository.existsById(procedureId)).thenReturn(true);
        when(procedureRepository.existsByIdAndHospitalId(procedureId, hospitalId)).thenReturn(true);
        when(fieldRepository.findMetricsFields(procedureId, null, hospitalId)).thenReturn(List.of());

        ProcedureMetricsResponse result = service.getProcedureMetrics(EMAIL, procedureId, START, END);

        assertThat(result.procedureId()).isEqualTo(procedureId);
        verify(fieldRepository).findMetricsFields(procedureId, null, hospitalId);
    }

    @Test
    void hospitalCannotAccessMetricsForAnotherHospitalsProcedure() {
        UUID procedureId = UUID.randomUUID();
        UUID hospitalId = UUID.randomUUID();
        User hospitalUser = user(Role.HOSPITAL);
        Hospital hospital = new Hospital();
        hospital.setId(hospitalId);
        hospitalUser.setHospital(hospital);
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
        when(procedureRepository.existsById(procedureId)).thenReturn(true);
        when(procedureRepository.existsByIdAndHospitalId(procedureId, hospitalId)).thenReturn(false);

        assertThatThrownBy(() -> service.getProcedureMetrics(EMAIL, procedureId, START, END))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("permissão para acessar");
    }

    @Test
    void procedureMetricsRejectsDateRangesOtherThanSevenDays() {
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(user(Role.ADMIN)));

        assertThatThrownBy(() -> service.getProcedureMetrics(
                EMAIL, UUID.randomUUID(), START, END.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 7 dias");
    }

    @Test
    void patientsCannotAccessPresentationMetrics() {
        when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(user(Role.PATIENT)));

        assertThatThrownBy(() -> service.getDailyAlerts(EMAIL, START, END))
                .isInstanceOf(UnauthorizedException.class);
    }

    private User user(Role role) {
        User user = new User(EMAIL, "unused", role);
        user.setId(UUID.randomUUID());
        return user;
    }

    private DoctorProcedureField field(UUID id, FieldDataType dataType) {
        DoctorProcedureField field = new DoctorProcedureField();
        field.setId(id);
        field.setName("Field " + id);
        field.setDataType(dataType);
        field.setActive(true);
        return field;
    }
}
