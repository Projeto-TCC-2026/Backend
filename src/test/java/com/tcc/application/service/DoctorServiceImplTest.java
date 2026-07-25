package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DoctorServiceImplTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private DoctorServiceImpl doctorService;

    private User user;
    private Hospital hospital;
    private Doctor doctor;
    private DoctorRequest request;
    private DoctorResponse response;

    @BeforeEach
    void setUp() {
        user = new User("doctor@test.com", "encoded", Role.DOCTOR);
        user.setId(1L);

        hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setId(1L);

        doctor = new Doctor(user, hospital, "Dr. Carlos", "11122233344", "CRM12345");
        doctor.setId(1L);
        doctor.setSpecialty("Cardiologia");

        request = new DoctorRequest(1L, 1L, "Dr. Carlos", "11122233344", "CRM12345", "Cardiologia", "11988887777");

        response = new DoctorResponse(1L, null, null, "Dr. Carlos", "11122233344", "CRM12345", "Cardiologia", "11988887777", null, null);
    }

    @Nested
    @DisplayName("createDoctor")
    class CreateDoctor {

        @Test
        @DisplayName("deve criar doutor com sucesso")
        void shouldCreateDoctorSuccessfully() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(false);
            when(doctorRepository.existsByCrm("CRM12345")).thenReturn(false);
            when(doctorRepository.findByUserId(1L)).thenReturn(Optional.empty());
            when(doctorMapper.toEntity(request, user, hospital)).thenReturn(doctor);
            when(doctorRepository.save(doctor)).thenReturn(doctor);
            when(doctorMapper.toResponse(doctor)).thenReturn(response);

            DoctorResponse result = doctorService.createDoctor(request);

            assertThat(result).isEqualTo(response);
            verify(doctorRepository).save(doctor);
        }

        @Test
        @DisplayName("deve lancar excecao quando CPF duplicado")
        void shouldThrowWhenDuplicateCpf() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(true);

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando CRM duplicado")
        void shouldThrowWhenDuplicateCrm() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(false);
            when(doctorRepository.existsByCrm("CRM12345")).thenReturn(true);

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CRM");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario ja associado a outro doutor")
        void shouldThrowWhenUserAlreadyAssociated() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(1L)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(false);
            when(doctorRepository.existsByCrm("CRM12345")).thenReturn(false);
            when(doctorRepository.findByUserId(1L)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Usuário já está associado");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao encontrado")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }

        @Test
        @DisplayName("deve lancar excecao quando hospital nao encontrado")
        void shouldThrowWhenHospitalNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hospital não encontrado");
        }
    }

    @Nested
    @DisplayName("deleteDoctor")
    class DeleteDoctor {

        @Test
        @DisplayName("deve excluir doutor sem relacionamentos")
        void shouldDeleteDoctorWithoutRelationships() {
            doctor.setDoctorPatients(new ArrayList<>());
            doctor.setProcedures(new ArrayList<>());
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

            doctorService.deleteDoctor(1L);

            verify(doctorRepository).delete(doctor);
        }

        @Test
        @DisplayName("deve lancar excecao quando ha pacientes associados")
        void shouldThrowWhenHasAssociatedPatients() {
            List<DoctorPatient> patients = List.of(new DoctorPatient());
            doctor.setDoctorPatients(new ArrayList<>(patients));
            doctor.setProcedures(new ArrayList<>());
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.deleteDoctor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pacientes associados");

            verify(doctorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando ha procedimentos associados")
        void shouldThrowWhenHasAssociatedProcedures() {
            doctor.setDoctorPatients(new ArrayList<>());
            List<Procedure> procedures = List.of(new Procedure());
            doctor.setProcedures(new ArrayList<>(procedures));
            when(doctorRepository.findById(1L)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.deleteDoctor(1L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("procedimentos associados");

            verify(doctorRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando doutor nao encontrado")
        void shouldThrowWhenDoctorNotFound() {
            when(doctorRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.deleteDoctor(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
