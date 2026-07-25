package com.tcc.application.service;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.application.mapper.ProcedureExecutionMapper;
import com.tcc.domain.model.HealthReading;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.ProcedureExecution;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PatientServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProcedureExecutionRepository procedureExecutionRepository;

    @Mock
    private PatientMapper patientMapper;

    @Mock
    private ProcedureExecutionMapper procedureExecutionMapper;

    @InjectMocks
    private PatientServiceImpl patientService;

    private User user;
    private Patient patient;
    private PatientRequest request;
    private PatientResponse response;

    @BeforeEach
    void setUp() {
        user = new User("patient@test.com", "encoded", Role.PATIENT);
        user.setId(1L);

        patient = new Patient(user, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1));
        patient.setId(1L);
        patient.setEmail("patient@test.com");

        request = new PatientRequest(
                1L, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1),
                "M", "11999999999", "patient@test.com", "Rua A",
                "Sao Paulo", "SP", "01000000", "O+", 70.0, 1.75
        );

        response = new PatientResponse(
                1L, null, "Joao Silva", "12345678901", LocalDate.of(1990, 1, 1),
                "M", "11999999999", "patient@test.com", "Rua A",
                "Sao Paulo", "SP", "01000000", "O+", 70.0, 1.75,
                true, null, null
        );
    }

    @Nested
    @DisplayName("createPatient")
    class CreatePatient {

        @Test
        @DisplayName("deve criar paciente com sucesso")
        void shouldCreatePatientSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(patientRepository.existsByEmailAndActiveTrue("patient@test.com")).thenReturn(false);
            when(patientMapper.toEntity(request, user)).thenReturn(patient);
            when(patientRepository.save(patient)).thenReturn(patient);
            when(patientMapper.toResponse(patient)).thenReturn(response);

            PatientResponse result = patientService.createPatient(request);

            assertThat(result).isEqualTo(response);
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando CPF duplicado")
        void shouldThrowWhenDuplicateCpf() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando email duplicado")
        void shouldThrowWhenDuplicateEmail() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(patientRepository.existsByEmailAndActiveTrue("patient@test.com")).thenReturn(true);

            assertThatThrownBy(() -> patientService.createPatient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("e-mail");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario ja associado a outro paciente")
        void shouldThrowWhenUserAlreadyAssociated() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(patientRepository.existsByCpfAndActiveTrue("12345678901")).thenReturn(false);
            when(patientRepository.findByUserId(1L)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.createPatient(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Usuário já está associado");

            verify(patientRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao encontrado")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.createPatient(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado");
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
            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            patientService.deletePatient(1L);

            verify(patientRepository).delete(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando ha ProcedureExecution associados")
        void shouldThrowWhenHasProcedureExecutions() {
            List<ProcedureExecution> executions = List.of(new ProcedureExecution());
            patient.setProcedureExecutions(new ArrayList<>(executions));
            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.deletePatient(1L))
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
            when(patientRepository.findById(1L)).thenReturn(Optional.of(patient));

            assertThatThrownBy(() -> patientService.deletePatient(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("leituras de saúde");

            verify(patientRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.deletePatient(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("inactivatePatient")
    class InactivatePatient {

        @Test
        @DisplayName("deve inativar paciente com sucesso")
        void shouldInactivatePatientSuccessfully() {
            when(patientRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(patient));
            when(patientRepository.save(patient)).thenReturn(patient);

            patientService.inactivatePatient(1L);

            assertThat(patient.getActive()).isFalse();
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("deve lancar excecao quando paciente nao encontrado")
        void shouldThrowWhenPatientNotFound() {
            when(patientRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> patientService.inactivatePatient(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
