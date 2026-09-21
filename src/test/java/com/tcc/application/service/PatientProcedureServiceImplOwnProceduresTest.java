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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.dto.response.PatientProcedureSummaryResponse;
import com.tcc.application.mapper.PatientProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;

/**
 * Cobre o fluxo do paciente no aplicativo: listar os próprios procedimentos a partir
 * do e-mail autenticado, sem receber identificador de paciente.
 */
@ExtendWith(MockitoExtension.class)
class PatientProcedureServiceImplOwnProceduresTest {

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

    private User patientUser;
    private Patient patient;

    private static final String EMAIL = "patient@tcc.com";
    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID PATIENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        patientUser = new User(EMAIL, "hash", Role.PATIENT);
        patientUser.setId(USER_ID);

        patient = new Patient();
        patient.setId(PATIENT_ID);
        patient.setUser(patientUser);
        patient.setActive(true);
    }

    private void mockAuthenticatedPatient() {
        when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(patientUser));
        when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.of(patient));
    }

    @Nested
    @DisplayName("listOwnProcedures")
    class ListOwnProcedures {

        @Test
        @DisplayName("deve retornar os procedimentos do paciente autenticado")
        void shouldReturnOwnProcedures() {
            PatientProcedure assignment = new PatientProcedure();
            assignment.setId(UUID.randomUUID());
            assignment.setPatient(patient);

            PatientProcedureSummaryResponse summary = new PatientProcedureSummaryResponse(
                    assignment.getId(), null, null, LocalDate.of(2026, 8, 10), null, "EM_ANDAMENTO");

            mockAuthenticatedPatient();
            when(patientProcedureRepository.findActiveByPatientUserEmail(EMAIL))
                    .thenReturn(List.of(assignment));
            when(patientProcedureMapper.toSummaryResponse(assignment)).thenReturn(summary);

            List<PatientProcedureSummaryResponse> result =
                    patientProcedureService.listOwnProcedures(EMAIL);

            assertThat(result).containsExactly(summary);
            verify(patientProcedureRepository).findActiveByPatientUserEmail(EMAIL);
        }

        @Test
        @DisplayName("deve retornar lista vazia quando o paciente nao tem procedimentos")
        void shouldReturnEmptyListWhenPatientHasNoProcedures() {
            mockAuthenticatedPatient();
            when(patientProcedureRepository.findActiveByPatientUserEmail(EMAIL))
                    .thenReturn(List.of());

            List<PatientProcedureSummaryResponse> result =
                    patientProcedureService.listOwnProcedures(EMAIL);

            assertThat(result).isEmpty();
            verifyNoInteractions(patientProcedureMapper);
        }

        @Test
        @DisplayName("deve lancar excecao quando o e-mail nao tem paciente associado")
        void shouldThrowWhenEmailHasNoPatient() {
            when(userRepository.findByEmailAndActiveTrue(EMAIL)).thenReturn(Optional.of(patientUser));
            when(patientRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientProcedureService.listOwnProcedures(EMAIL))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(patientProcedureRepository, never()).findActiveByPatientUserEmail(EMAIL);
        }
    }
}
