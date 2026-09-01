package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.application.dto.request.ManualCheckinRequest;
import com.tcc.application.dto.response.CheckinResponse;
import com.tcc.domain.model.Checkin;
import com.tcc.domain.model.CheckinSource;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldDataType;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CheckinServiceImplTest {

    private static final String EMAIL = "patient@tcc.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PATIENT_PROCEDURE_ID = UUID.randomUUID();
    private static final UUID FIELD_ID = UUID.randomUUID();

    @Mock private UserRepository userRepository;
    @Mock private PatientRepository patientRepository;
    @Mock private PatientProcedureRepository patientProcedureRepository;
    @Mock private DoctorProcedureRepository doctorProcedureRepository;
    @Mock private DoctorProcedureFieldRepository fieldRepository;
    @Mock private CheckinRepository checkinRepository;

    private CheckinService service;
    private PatientProcedure patientProcedure;
    private DoctorProcedureField field;

    @BeforeEach
    void setUp() {
        service = new CheckinServiceImpl(userRepository, patientRepository, patientProcedureRepository,
                doctorProcedureRepository, fieldRepository, checkinRepository);

        User user = new User(EMAIL, "hash", Role.PATIENT);
        user.setId(USER_ID);
        Patient patient = new Patient();
        patient.setId(UUID.randomUUID());
        Doctor doctor = new Doctor();
        doctor.setId(UUID.randomUUID());
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());

        patientProcedure = new PatientProcedure();
        patientProcedure.setId(PATIENT_PROCEDURE_ID);
        patientProcedure.setPatient(patient);
        patientProcedure.setDoctor(doctor);
        patientProcedure.setProcedure(procedure);
        patientProcedure.setStatus("EM_ANDAMENTO");
        patientProcedure.setStartDate(LocalDate.now().minusDays(1));

        DoctorProcedure doctorProcedure = new DoctorProcedure();
        doctorProcedure.setId(UUID.randomUUID());
        field = new DoctorProcedureField();
        field.setId(FIELD_ID);
        field.setName("Temperatura");
        field.setDataType(FieldDataType.DECIMAL);
        field.setRequired(true);
        field.setMinValue(35.0);
        field.setMaxValue(42.0);

        when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(user));
        when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.of(patient));
        when(patientProcedureRepository.findById(PATIENT_PROCEDURE_ID)).thenReturn(Optional.of(patientProcedure));
        lenient().when(doctorProcedureRepository.findByDoctorIdAndProcedureId(doctor.getId(), procedure.getId()))
                .thenReturn(Optional.of(doctorProcedure));
        lenient().when(fieldRepository.findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(doctorProcedure.getId()))
                .thenReturn(List.of(field));
    }

    @Test
    void submitsTypedRequiredFieldForOwnActiveProcedure() {
        when(checkinRepository.existsByPatientProcedureIdAndSourceAndManualDate(
                PATIENT_PROCEDURE_ID, CheckinSource.MANUAL, LocalDate.now())).thenReturn(false);
        when(checkinRepository.save(any(Checkin.class))).thenAnswer(invocation -> {
            Checkin checkin = invocation.getArgument(0);
            checkin.setId(UUID.randomUUID());
            return checkin;
        });

        CheckinResponse response = service.submitManual(EMAIL, PATIENT_PROCEDURE_ID,
                new ManualCheckinRequest(List.of(new ManualCheckinRequest.FieldValue(FIELD_ID, "36.5", null))));

        assertThat(response.id()).isNotNull();
        ArgumentCaptor<Checkin> saved = ArgumentCaptor.forClass(Checkin.class);
        verify(checkinRepository).save(saved.capture());
        assertThat(saved.getValue().getSource()).isEqualTo(CheckinSource.MANUAL);
        assertThat(saved.getValue().getManualDate()).isEqualTo(LocalDate.now());
        assertThat(saved.getValue().getFieldValues()).singleElement()
                .satisfies(value -> assertThat(value.getRawValue()).isEqualTo("36.5"));
    }

    @Test
    void rejectsSecondManualCheckinForSameDay() {
        when(checkinRepository.existsByPatientProcedureIdAndSourceAndManualDate(
                PATIENT_PROCEDURE_ID, CheckinSource.MANUAL, LocalDate.now())).thenReturn(true);

        assertThatThrownBy(() -> service.submitManual(EMAIL, PATIENT_PROCEDURE_ID,
                new ManualCheckinRequest(List.of())))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Já existe um check-in");
    }

    @Test
    void rejectsFieldOutsideProcedureConfiguration() {
        when(checkinRepository.existsByPatientProcedureIdAndSourceAndManualDate(
                PATIENT_PROCEDURE_ID, CheckinSource.MANUAL, LocalDate.now())).thenReturn(false);

        assertThatThrownBy(() -> service.submitManual(EMAIL, PATIENT_PROCEDURE_ID,
                new ManualCheckinRequest(List.of(
                        new ManualCheckinRequest.FieldValue(UUID.randomUUID(), "36.5", null)))))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("não está ativo");
    }
}
