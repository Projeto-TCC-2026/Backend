package com.tcc.application.service;

import java.time.LocalDate;
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

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.mapper.PatientProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class PatientProcedureServiceImplTest {

    @Mock
    private PatientProcedureRepository patientProcedureRepository;

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private DoctorProcedureRepository doctorProcedureRepository;

    @Mock
    private DoctorPatientRepository doctorPatientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PatientProcedureMapper patientProcedureMapper;

    @Mock
    private ProcedureMapper procedureMapper;

    @InjectMocks
    private PatientProcedureServiceImpl patientProcedureService;

    private User doctorUser;
    private Doctor doctor;
    private Patient patient;
    private Procedure procedure;
    private PatientProcedure assignment;
    private PatientProcedureRequest request;
    private PatientProcedureResponse response;

    private static final String EMAIL = "doctor@tcc.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();
    private static final UUID PROCEDURE_ID = UUID.randomUUID();
    private static final UUID ASSIGNMENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        Hospital hospital = new Hospital("Hospital São Lucas", "12345678000199");
        hospital.setId(UUID.randomUUID());

        doctorUser = new User(EMAIL, "hash", Role.DOCTOR);
        doctorUser.setId(USER_ID);

        doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setHospital(hospital);
        doctor.setFullName("Dr. Carlos Mendes");

        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setFullName("Ana Paula Silva");
        patient.setActive(true);

        procedure = new Procedure(hospital, "Artroscopia de ombro");
        procedure.setId(PROCEDURE_ID);
        procedure.setActive(true);

        assignment = new PatientProcedure();
        assignment.setId(ASSIGNMENT_ID);
        assignment.setPatient(patient);
        assignment.setProcedure(procedure);
        assignment.setDoctor(doctor);

        request = new PatientProcedureRequest(PROCEDURE_ID, LocalDate.of(2026, 8, 10),
                null, "EM_ANDAMENTO", "Primeira sessão");

        response = new PatientProcedureResponse(ASSIGNMENT_ID, null, null, null,
                LocalDate.of(2026, 8, 10), null, "EM_ANDAMENTO", "Primeira sessão");
    }

    private void mockAuthenticatedDoctor() {
        when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(doctorUser));
        when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(doctor));
    }

    private void mockLinkedPatient() {
        when(patientRepository.findByIdAndActiveTrue(PATIENT_ID)).thenReturn(Optional.of(patient));
        when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("listAvailableProcedures")
    class ListAvailableProcedures {

        @Test
        @DisplayName("deve retornar apenas procedimentos atribuidos ao medico")
        void shouldReturnOnlyAssignedProcedures() {
            ProcedureResponse procedureResponse = new ProcedureResponse(PROCEDURE_ID, null,
                    "Artroscopia de ombro", null, 90, true, null, List.of());

            mockAuthenticatedDoctor();
            when(procedureRepository.findActiveByDoctorId(DOCTOR_ID)).thenReturn(List.of(procedure));
            when(procedureMapper.toResponse(procedure)).thenReturn(procedureResponse);

            List<ProcedureResponse> result = patientProcedureService.listAvailableProcedures(EMAIL);

            assertThat(result).containsExactly(procedureResponse);
            verify(procedureRepository).findActiveByDoctorId(DOCTOR_ID);
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao tem perfil de medico")
        void shouldThrowWhenUserHasNoDoctorProfile() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(doctorUser));
            when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientProcedureService.listAvailableProcedures(EMAIL))
                    .isInstanceOf(UnauthorizedException.class);
        }
    }

    @Nested
    @DisplayName("assignProcedure")
    class AssignProcedure {

        @Test
        @DisplayName("deve atribuir procedimento ao paciente")
        void shouldAssignProcedureSuccessfully() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(true);
            when(patientProcedureRepository.existsByPatientIdAndProcedureIdAndDoctorId(
                    PATIENT_ID, PROCEDURE_ID, DOCTOR_ID)).thenReturn(false);
            when(patientProcedureMapper.toEntity(request, patient, procedure, doctor)).thenReturn(assignment);
            when(patientProcedureRepository.save(assignment)).thenReturn(assignment);
            when(patientProcedureMapper.toResponse(assignment)).thenReturn(response);

            PatientProcedureResponse result =
                    patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request);

            assertThat(result).isEqualTo(response);
            verify(patientProcedureRepository).save(assignment);
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento nao foi atribuido ao medico pelo hospital")
        void shouldThrowWhenProcedureNotAssignedToDoctor() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(patientProcedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento esta inativo")
        void shouldThrowWhenProcedureIsInactive() {
            procedure.setActive(false);
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request))
                    .isInstanceOf(BusinessException.class);

            verify(patientProcedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao esta vinculado ao medico")
        void shouldThrowWhenPatientNotLinkedToDoctor() {
            mockAuthenticatedDoctor();
            when(patientRepository.findByIdAndActiveTrue(PATIENT_ID)).thenReturn(Optional.of(patient));
            when(doctorPatientRepository.existsByDoctorIdAndPatientId(DOCTOR_ID, PATIENT_ID))
                    .thenReturn(false);

            assertThatThrownBy(() -> patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(patientProcedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento ja atribuido ao paciente")
        void shouldThrowWhenAlreadyAssigned() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(true);
            when(patientProcedureRepository.existsByPatientIdAndProcedureIdAndDoctorId(
                    PATIENT_ID, PROCEDURE_ID, DOCTOR_ID)).thenReturn(true);

            assertThatThrownBy(() -> patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request))
                    .isInstanceOf(BusinessException.class);

            verify(patientProcedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            mockAuthenticatedDoctor();
            when(patientRepository.findByIdAndActiveTrue(PATIENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientProcedureService.assignProcedure(EMAIL, PATIENT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("listPatientProcedures")
    class ListPatientProcedures {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("deve listar apenas atribuicoes do medico autenticado")
        void shouldListOnlyOwnAssignments() {
            Page<PatientProcedure> page = new PageImpl<>(List.of(assignment));
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(patientProcedureRepository.findByPatientIdAndDoctorId(PATIENT_ID, DOCTOR_ID, pageable))
                    .thenReturn(page);
            when(patientProcedureMapper.toResponse(assignment)).thenReturn(response);

            Page<PatientProcedureResponse> result =
                    patientProcedureService.listPatientProcedures(EMAIL, PATIENT_ID, pageable);

            assertThat(result.getContent()).containsExactly(response);
        }
    }

    @Nested
    @DisplayName("updateAssignment")
    class UpdateAssignment {

        @Test
        @DisplayName("deve atualizar atribuicao propria")
        void shouldUpdateOwnAssignment() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(patientProcedureRepository.findByIdAndPatientIdAndDoctorId(
                    ASSIGNMENT_ID, PATIENT_ID, DOCTOR_ID)).thenReturn(Optional.of(assignment));
            when(patientProcedureRepository.save(assignment)).thenReturn(assignment);
            when(patientProcedureMapper.toResponse(assignment)).thenReturn(response);

            PatientProcedureResponse result = patientProcedureService.updateAssignment(
                    EMAIL, PATIENT_ID, ASSIGNMENT_ID, request);

            assertThat(result).isEqualTo(response);
            verify(patientProcedureMapper).updateEntity(assignment, request);
        }

        @Test
        @DisplayName("deve lancar excecao quando atribuicao e de outro medico")
        void shouldThrowWhenAssignmentBelongsToAnotherDoctor() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(patientProcedureRepository.findByIdAndPatientIdAndDoctorId(
                    ASSIGNMENT_ID, PATIENT_ID, DOCTOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientProcedureService.updateAssignment(
                    EMAIL, PATIENT_ID, ASSIGNMENT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(patientProcedureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("removeAssignment")
    class RemoveAssignment {

        @Test
        @DisplayName("deve remover atribuicao propria")
        void shouldRemoveOwnAssignment() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(patientProcedureRepository.findByIdAndPatientIdAndDoctorId(
                    ASSIGNMENT_ID, PATIENT_ID, DOCTOR_ID)).thenReturn(Optional.of(assignment));

            patientProcedureService.removeAssignment(EMAIL, PATIENT_ID, ASSIGNMENT_ID);

            verify(patientProcedureRepository).delete(assignment);
        }

        @Test
        @DisplayName("deve lancar excecao quando atribuicao nao encontrada")
        void shouldThrowWhenAssignmentNotFound() {
            mockAuthenticatedDoctor();
            mockLinkedPatient();
            when(patientProcedureRepository.findByIdAndPatientIdAndDoctorId(
                    ASSIGNMENT_ID, PATIENT_ID, DOCTOR_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientProcedureService.removeAssignment(
                    EMAIL, PATIENT_ID, ASSIGNMENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(patientProcedureRepository, never()).delete(any());
        }
    }
}
