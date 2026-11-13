package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.domain.model.CheckinSource;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldDataType;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
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
class PatientHomeMetricsServiceImplTest {
    private static final String EMAIL = "patient@example.com";
    private static final LocalDate START = LocalDate.of(2026, 11, 2);
    private static final LocalDate END = START.plusDays(6);

    @Mock
    private PatientRepository patientRepository;
    @Mock
    private PatientProcedureRepository patientProcedureRepository;
    @Mock
    private DoctorProcedureRepository doctorProcedureRepository;
    @Mock
    private DoctorProcedureFieldRepository fieldRepository;
    @Mock
    private CheckinRepository checkinRepository;

    private PatientHomeMetricsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PatientHomeMetricsServiceImpl(
                patientRepository, patientProcedureRepository, doctorProcedureRepository,
                fieldRepository, checkinRepository);
    }

    @Test
    void returnsSevenDaySubmissionAndNumericFieldTrendsForTheAuthenticatedPatient() {
        UUID patientId = UUID.randomUUID();
        UUID patientProcedureId = UUID.randomUUID();
        UUID doctorProcedureId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        UUID procedureId = UUID.randomUUID();
        UUID fieldId = UUID.randomUUID();
        Patient patient = new Patient();
        patient.setId(patientId);
        patient.setEmail(EMAIL);
        Procedure procedure = new Procedure();
        procedure.setId(procedureId);
        procedure.setTitle("Recuperação");
        Doctor doctor = new Doctor();
        doctor.setId(doctorId);
        PatientProcedure assignment = new PatientProcedure(patient, procedure, doctor, START.minusDays(10), "ACTIVE");
        assignment.setId(patientProcedureId);
        DoctorProcedure doctorProcedure = new DoctorProcedure();
        doctorProcedure.setId(doctorProcedureId);
        DoctorProcedureField numericField = new DoctorProcedureField();
        numericField.setId(fieldId);
        numericField.setName("Dor");
        numericField.setDataType(FieldDataType.SCALE);
        numericField.setUnit("escala");
        DoctorProcedureField textField = new DoctorProcedureField();
        textField.setId(UUID.randomUUID());
        textField.setName("Observação");
        textField.setDataType(FieldDataType.TEXT);
        when(patientRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(patient));
        when(patientProcedureRepository.findByIdAndActiveTrue(patientProcedureId)).thenReturn(Optional.of(assignment));
        when(checkinRepository.findDailyManualCheckinDates(patientId, START, END, CheckinSource.MANUAL))
                .thenReturn(List.of(START.plusDays(2)));
        when(doctorProcedureRepository.findByDoctorIdAndProcedureIdAndActiveTrue(doctorId, procedureId))
                .thenReturn(Optional.of(doctorProcedure));
        when(fieldRepository.findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(doctorProcedureId))
                .thenReturn(List.of(numericField, textField));
        when(checkinRepository.findManualNumericFieldValues(
                patientId, patientProcedureId, List.of(fieldId),
                List.of(FieldDataType.INTEGER, FieldDataType.DECIMAL, FieldDataType.SCALE),
                START, END, CheckinSource.MANUAL))
                .thenReturn(List.<Object[]>of(
                        new Object[] { fieldId, START.plusDays(2), "4" },
                        new Object[] { fieldId, START.plusDays(2), "8" }));
        when(checkinRepository.existsManualCheckinByPatientIdAndManualDate(
                org.mockito.ArgumentMatchers.eq(patientId),
                org.mockito.ArgumentMatchers.eq(CheckinSource.MANUAL),
                org.mockito.ArgumentMatchers.any(LocalDate.class))).thenReturn(true);

        var result = service.getHomeMetrics(EMAIL, patientProcedureId, START, END);

        assertThat(result.patientProcedureId()).isEqualTo(patientProcedureId);
        assertThat(result.procedureTitle()).isEqualTo("Recuperação");
        assertThat(result.submittedToday()).isTrue();
        assertThat(result.dailyCheckins()).hasSize(7);
        assertThat(result.dailyCheckins().get(2).submitted()).isTrue();
        assertThat(result.dailyCheckins().get(0).submitted()).isFalse();
        assertThat(result.numericFields()).hasSize(1);
        assertThat(result.numericFields().get(0).dailyAverages()).hasSize(7);
        assertThat(result.numericFields().get(0).dailyAverages().get(2).average()).isEqualByComparingTo("6");
        assertThat(result.numericFields().get(0).dailyAverages().get(2).sampleSize()).isEqualTo(2);
        verify(checkinRepository).existsManualCheckinByPatientIdAndManualDate(
                org.mockito.ArgumentMatchers.eq(patientId),
                org.mockito.ArgumentMatchers.eq(CheckinSource.MANUAL),
                org.mockito.ArgumentMatchers.any(LocalDate.class));
    }

    @Test
    void doesNotExposeAnotherPatientsProcedure() {
        UUID procedureId = UUID.randomUUID();
        Patient authenticatedPatient = new Patient();
        authenticatedPatient.setId(UUID.randomUUID());
        Patient otherPatient = new Patient();
        otherPatient.setId(UUID.randomUUID());
        PatientProcedure otherPatientsAssignment = new PatientProcedure(
                otherPatient, new Procedure(), new Doctor(), START, "ACTIVE");
        when(patientRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(authenticatedPatient));
        when(patientProcedureRepository.findByIdAndActiveTrue(procedureId))
                .thenReturn(Optional.of(otherPatientsAssignment));

        assertThatThrownBy(() -> service.getHomeMetrics(EMAIL, procedureId, START, END))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Acompanhamento não encontrado");
    }

    @Test
    void rejectsRangesThatDoNotContainSevenInclusiveDays() {
        assertThatThrownBy(() -> service.getHomeMetrics(EMAIL, UUID.randomUUID(), START, END.minusDays(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exatamente 7 dias");
    }
}
