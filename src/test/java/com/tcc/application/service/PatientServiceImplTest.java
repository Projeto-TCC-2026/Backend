package com.tcc.application.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.application.mapper.ProcedureExecutionMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ProcedureExecution;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private DoctorPatientRepository doctorPatientRepository;

    @Mock
    private ProcedureExecutionRepository procedureExecutionRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private ProcedureExecutionMapper procedureExecutionMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private User user;
    private User doctorUser;
    private Doctor doctor;
    private Patient patient;
    private PatientRequest request;
    private PatientResponse response;

    private static final String DOCTOR_EMAIL = "doctor@tcc.com";
    private static final String ADMIN_EMAIL = "admin@tcc.com";
    private static final String HOSPITAL_EMAIL = "hospital@tcc.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOCTOR_USER_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID HOSPITAL_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User("patient@test.com", "encoded", Role.PATIENT);
        user.setId(USER_ID);

        doctorUser = new User(DOCTOR_EMAIL, "encoded", Role.DOCTOR);
        doctorUser.setId(DOCTOR_USER_ID);

        doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setFullName("Dr. Carlos Mendes");

        patient = new Patient(user, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1));
        patient.setId(PATIENT_ID);
        patient.setEmail("patient@test.com");

        request = new PatientRequest(
                USER_ID, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1),
                "M", "11999999999", "patient@test.com", "Rua A",
                "Sao Paulo", "SP", "01000000", "O+", 70.0, 1.75
        );

        response = new PatientResponse(
                PATIENT_ID, null, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1),
                "M", "11999999999", "patient@test.com", "Rua A",
                "Sao Paulo", "SP", "01000000", "O+", 70.0, 1.75,
                true, null, null
        );
    }

    @Nested
    @DisplayName("createPatient")
    class CreatePatient {

        @Test
        @DisplayName("deve criar paciente e vincular ao medico autenticado")
        void shouldCreatePatientSuccessfully() {
            mockAuthenticatedDoctor();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(patientRepository.existsByEmailAndActiveTrue("patient@test.com")).thenReturn(false);
            when(patientMapper.toEntity(request, user)).thenReturn(patient);
            when(patientRepository.save(patient)).thenReturn(patient);
            when(patientMapper.toResponse(patient)).thenReturn(response);

            PatientResponse result = patientService.createPatient(DOCTOR_EMAIL, request);

            assertThat(result).isEqualTo(response);
            verify(patientRepository).save(patient);

            ArgumentCaptor<DoctorPatient> captor = ArgumentCaptor.forClass(DoctorPatient.class);
            verify(doctorPatientRepository).save(captor.capture());
            assertThat(captor.getValue().getDoctor()).isEqualTo(doctor);
            assertThat(captor.getValue().getPatient()).isEqualTo(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario autenticado nao tem perfil de medico")
        void shouldThrowWhenAuthenticatedUserIsNotDoctor() {
            when(userRepository.findByEmailAndActiveTrue(DOCTOR_EMAIL)).thenReturn(Optional.of(doctorUser));
            when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.createPatient(DOCTOR_EMAIL, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(patientRepository, never()).save(any());
            verify(doctorPatientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando CPF duplicado")
        void shouldThrowWhenDuplicateCpf() {
            mockAuthenticatedDoctor();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(DOCTOR_EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando email duplicado")
        void shouldThrowWhenDuplicateEmail() {
            mockAuthenticatedDoctor();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
            when(patientRepository.existsByEmailAndActiveTrue("patient@test.com")).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(DOCTOR_EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario ja associado a outro paciente")
        void shouldThrowWhenUserAlreadyAssociated() {
            mockAuthenticatedDoctor();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.createPatient(DOCTOR_EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Usuário já está associado");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao encontrado")
        void shouldThrowWhenUserNotFound() {
            mockAuthenticatedDoctor();
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.createPatient(DOCTOR_EMAIL, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }

        private void mockAuthenticatedDoctor() {
            when(userRepository.findByEmailAndActiveTrue(DOCTOR_EMAIL)).thenReturn(Optional.of(doctorUser));
            when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.of(doctor));
        }
    }

    @Nested
    @DisplayName("getAllActivePatients")
    class GetAllActivePatients {

        @Test
        @DisplayName("admin lista todos os pacientes ativos")
        void adminListsAllPatients() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser()));
            when(patientRepository.findPagedByActiveTrue(pageable))
                    .thenReturn(new PageImpl<>(List.of(patient)));
            when(patientMapper.toResponse(patient)).thenReturn(response);

            Page<PatientResponse> result = patientService.getAllActivePatients(ADMIN_EMAIL, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(patientRepository).findPagedByActiveTrue(pageable);
        }

        @Test
        @DisplayName("doutor lista apenas os seus pacientes")
        void doctorListsOwnPatients() {
            Pageable pageable = PageRequest.of(0, 10);
            mockAuthenticatedDoctor();
            when(patientRepository.findPagedActiveByDoctorId(DOCTOR_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(patient)));
            when(patientMapper.toResponse(patient)).thenReturn(response);

            Page<PatientResponse> result = patientService.getAllActivePatients(DOCTOR_EMAIL, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(patientRepository).findPagedActiveByDoctorId(DOCTOR_ID, pageable);
        }

        @Test
        @DisplayName("hospital lista apenas pacientes dos medicos do seu hospital")
        void hospitalListsPatientsOfItsDoctors() {
            Pageable pageable = PageRequest.of(0, 10);
            when(userRepository.findByEmailAndActiveTrue(HOSPITAL_EMAIL)).thenReturn(Optional.of(hospitalUser()));
            when(patientRepository.findPagedActiveByHospitalId(HOSPITAL_ID, pageable))
                    .thenReturn(new PageImpl<>(List.of(patient)));
            when(patientMapper.toResponse(patient)).thenReturn(response);

            Page<PatientResponse> result = patientService.getAllActivePatients(HOSPITAL_EMAIL, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(patientRepository).findPagedActiveByHospitalId(HOSPITAL_ID, pageable);
        }
    }

    @Nested
    @DisplayName("getPatientById")
    class GetPatientById {

        @Test
        @DisplayName("doutor nao acessa paciente de outro medico")
        void doctorCannotAccessUnlinkedPatient() {
            mockAuthenticatedDoctor();
            when(patientRepository.findByIdAndActiveTrue(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID)).thenReturn(false);

            assertThatThrownBy(() -> patientService.getPatientById(DOCTOR_EMAIL, PATIENT_ID))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("vinculado");
        }
    }

    @Nested
    @DisplayName("deletePatient")
    class DeletePatient {

        @Test
        @DisplayName("deve excluir paciente sem relacionamentos")
        void shouldDeletePatientWithoutRelationships() {
            patient.setProcedureExecutions(new ArrayList<>());
            patient.setHealthReadings(new ArrayList<>());
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

            patientService.deletePatient(PATIENT_ID);

            verify(patientRepository).delete(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando ha ProcedureExecution associados")
        void shouldThrowWhenHasProcedureExecutions() {
            List<ProcedureExecution> executions = List.of(new ProcedureExecution());
            patient.setProcedureExecutions(new ArrayList<>(executions));
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.deletePatient(PATIENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("procedimento");

            verify(patientRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando ha HealthReading associados")
        void shouldThrowWhenHasHealthReadings() {
            patient.setProcedureExecutions(new ArrayList<>());
            List<HealthReading> readings = List.of(new HealthReading());
            patient.setHealthReadings(new ArrayList<>(readings));
            when(patientRepository.findById(PATIENT_ID)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.deletePatient(PATIENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("leituras de saúde");

            verify(patientRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.deletePatient(NONEXISTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("inactivatePatient")
    class InactivatePatient {

        @Test
        @DisplayName("deve inativar paciente com sucesso")
        void shouldInactivatePatientSuccessfully() {
            when(patientRepository.findByIdAndActiveTrue(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(userRepository.findByEmailAndActiveTrue(ADMIN_EMAIL)).thenReturn(Optional.of(adminUser()));
            when(patientRepository.save(patient)).thenReturn(patient);

            patientService.inactivatePatient(ADMIN_EMAIL, PATIENT_ID);

            assertThat(patient.getActive()).isFalse();
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findByIdAndActiveTrue(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.inactivatePatient(ADMIN_EMAIL, NONEXISTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    private void mockAuthenticatedDoctor() {
        when(userRepository.findByEmailAndActiveTrue(DOCTOR_EMAIL)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(DOCTOR_USER_ID)).thenReturn(Optional.of(doctor));
    }

    private User adminUser() {
        return new User(ADMIN_EMAIL, "encoded", Role.ADMIN);
    }

    private User hospitalUser() {
        Hospital hospital = new Hospital();
        hospital.setId(HOSPITAL_ID);
        User hospitalUser = new User(HOSPITAL_EMAIL, "encoded", Role.HOSPITAL);
        hospitalUser.setHospital(hospital);
        return hospitalUser;
    }
}
