package com.tcc.application.service;

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
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;

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

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID HOSPITAL_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        user = new User("doctor@test.com", "encoded", Role.DOCTOR);
        user.setId(USER_ID);

        hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setId(HOSPITAL_ID);

        doctor = new Doctor(user, hospital, "Dr. Carlos", "11122233344", "CRM12345");
        doctor.setId(DOCTOR_ID);
        doctor.setSpecialty("Cardiologia");

        request = new DoctorRequest(USER_ID, HOSPITAL_ID, "Dr. Carlos", "11122233344", "CRM12345", "Cardiologia", "11988887777");

        response = new DoctorResponse(DOCTOR_ID, null, null, "Dr. Carlos", "11122233344", "CRM12345", true, "Cardiologia", "11988887777", null, null);
    }

    @Nested
    @DisplayName("createDoctor")
    class CreateDoctor {

        @Test
        @DisplayName("deve criar doutor com sucesso")
        void shouldCreateDoctorSuccessfully() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(false);
            when(doctorRepository.existsByCrm("CRM12345")).thenReturn(false);
            when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());
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
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(true);

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CPF");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando CRM duplicado")
        void shouldThrowWhenDuplicateCrm() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));
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
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));
            when(doctorRepository.existsByCpf("11122233344")).thenReturn(false);
            when(doctorRepository.existsByCrm("CRM12345")).thenReturn(false);
            when(doctorRepository.findByUserId(USER_ID)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Usuário já está associado");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao encontrado")
        void shouldThrowWhenUserNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Usuário não encontrado");
        }

        @Test
        @DisplayName("deve lancar excecao quando hospital nao encontrado")
        void shouldThrowWhenHospitalNotFound() {
            when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.createDoctor(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Hospital não encontrado");
        }
    }

    @Nested
    @DisplayName("deleteDoctor")
    class DeleteDoctor {

        @Test
        @DisplayName("deve inativar doutor e conta associada sem excluir registros")
        void shouldDeleteDoctorAndAssociatedUserWithoutRelationships() {
            doctor.setDoctorPatients(new ArrayList<>());
            doctor.setDoctorProcedures(new ArrayList<>());
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

            doctorService.deleteDoctor(DOCTOR_ID);

            verify(doctorRepository).save(doctor);
            verify(userRepository).save(user);
            assertThat(doctor.getActive()).isFalse();
            assertThat(user.getActive()).isFalse();
        }

        @Test
        @DisplayName("deve lancar excecao quando ha pacientes associados")
        void shouldThrowWhenHasAssociatedPatients() {
            List<DoctorPatient> patients = List.of(new DoctorPatient());
            doctor.setDoctorPatients(new ArrayList<>(patients));
            doctor.setDoctorProcedures(new ArrayList<>());
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.deleteDoctor(DOCTOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("pacientes associados");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando ha procedimentos associados")
        void shouldThrowWhenHasAssociatedProcedures() {
            doctor.setDoctorPatients(new ArrayList<>());
            List<DoctorProcedure> doctorProcedures = List.of(new DoctorProcedure());
            doctor.setDoctorProcedures(new ArrayList<>(doctorProcedures));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> doctorService.deleteDoctor(DOCTOR_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("procedimentos associados");

            verify(doctorRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando doutor nao encontrado")
        void shouldThrowWhenDoctorNotFound() {
            when(doctorRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> doctorService.deleteDoctor(NONEXISTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
