package com.tcc.application.service;

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

import com.tcc.application.dto.request.DoctorProcedureRequest;
import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.mapper.DoctorProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class ProcedureServiceImplTest {

    @Mock
    private ProcedureRepository procedureRepository;

    @Mock
    private DoctorProcedureRepository doctorProcedureRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProcedureMapper procedureMapper;

    @Mock
    private DoctorProcedureMapper doctorProcedureMapper;

    @InjectMocks
    private ProcedureServiceImpl procedureService;

    private Hospital hospital;
    private Hospital otherHospital;
    private User hospitalUser;
    private Doctor doctor;
    private Procedure procedure;
    private ProcedureRequest request;
    private ProcedureResponse response;

    private static final String EMAIL = "hospital@tcc.com";
    private static final UUID HOSPITAL_ID = UUID.randomUUID();
    private static final UUID OTHER_HOSPITAL_ID = UUID.randomUUID();
    private static final UUID PROCEDURE_ID = UUID.randomUUID();
    private static final UUID DOCTOR_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        hospital = new Hospital("Hospital São Lucas", "12345678000199");
        hospital.setId(HOSPITAL_ID);

        otherHospital = new Hospital("Hospital Santa Casa", "98765432000188");
        otherHospital.setId(OTHER_HOSPITAL_ID);

        hospitalUser = new User(EMAIL, "hash", Role.HOSPITAL);
        hospitalUser.setHospital(hospital);

        doctor = new Doctor();
        doctor.setId(DOCTOR_ID);
        doctor.setHospital(hospital);
        doctor.setFullName("Dr. Ana Souza");

        procedure = new Procedure(hospital, "Cirurgia de joelho");
        procedure.setId(PROCEDURE_ID);
        procedure.setActive(true);

        request = new ProcedureRequest("Cirurgia de joelho", "Artroplastia total", 120, null);

        response = new ProcedureResponse(PROCEDURE_ID, null, "Cirurgia de joelho",
                "Artroplastia total", 120, true, null, List.of());
    }

    @Nested
    @DisplayName("createProcedure")
    class CreateProcedure {

        @Test
        @DisplayName("deve criar procedimento no hospital do usuario autenticado")
        void shouldCreateProcedureSuccessfully() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.existsByHospitalIdAndTitleIgnoreCase(HOSPITAL_ID, "Cirurgia de joelho"))
                    .thenReturn(false);
            when(procedureMapper.toEntity(request, hospital)).thenReturn(procedure);
            when(procedureRepository.save(procedure)).thenReturn(procedure);
            when(procedureMapper.toResponse(procedure)).thenReturn(response);

            ProcedureResponse result = procedureService.createProcedure(EMAIL, request);

            assertThat(result).isEqualTo(response);
            verify(procedureRepository).save(procedure);
        }

        @Test
        @DisplayName("deve lancar excecao quando titulo duplicado no hospital")
        void shouldThrowWhenDuplicateTitle() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.existsByHospitalIdAndTitleIgnoreCase(HOSPITAL_ID, "Cirurgia de joelho"))
                    .thenReturn(true);

            assertThatThrownBy(() -> procedureService.createProcedure(EMAIL, request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("Cirurgia de joelho");

            verify(procedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando usuario nao esta vinculado a hospital")
        void shouldThrowWhenUserHasNoHospital() {
            User orphanUser = new User(EMAIL, "hash", Role.HOSPITAL);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(orphanUser));

            assertThatThrownBy(() -> procedureService.createProcedure(EMAIL, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(procedureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("listProcedures")
    class ListProcedures {

        private final Pageable pageable = PageRequest.of(0, 10);

        @Test
        @DisplayName("deve listar apenas procedimentos ativos por padrao")
        void shouldListOnlyActiveByDefault() {
            Page<Procedure> page = new PageImpl<>(List.of(procedure));
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findByHospitalIdAndActive(HOSPITAL_ID, true, pageable)).thenReturn(page);
            when(procedureMapper.toResponse(procedure)).thenReturn(response);

            Page<ProcedureResponse> result = procedureService.listProcedures(EMAIL, false, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(procedureRepository, never()).findByHospitalId(any(), any());
        }

        @Test
        @DisplayName("deve incluir inativos quando solicitado")
        void shouldIncludeInactiveWhenRequested() {
            Page<Procedure> page = new PageImpl<>(List.of(procedure));
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findByHospitalId(HOSPITAL_ID, pageable)).thenReturn(page);
            when(procedureMapper.toResponse(procedure)).thenReturn(response);

            Page<ProcedureResponse> result = procedureService.listProcedures(EMAIL, true, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(procedureRepository, never()).findByHospitalIdAndActive(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("updateProcedure")
    class UpdateProcedure {

        @Test
        @DisplayName("deve atualizar procedimento do proprio hospital")
        void shouldUpdateProcedureSuccessfully() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(procedureRepository.save(procedure)).thenReturn(procedure);
            when(procedureMapper.toResponse(procedure)).thenReturn(response);

            ProcedureResponse result = procedureService.updateProcedure(EMAIL, PROCEDURE_ID, request);

            assertThat(result).isEqualTo(response);
            verify(procedureMapper).updateEntity(procedure, request);
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento e de outro hospital")
        void shouldThrowWhenProcedureBelongsToAnotherHospital() {
            procedure.setHospital(otherHospital);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));

            assertThatThrownBy(() -> procedureService.updateProcedure(EMAIL, PROCEDURE_ID, request))
                    .isInstanceOf(UnauthorizedException.class);

            verify(procedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento nao encontrado")
        void shouldThrowWhenProcedureNotFound() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> procedureService.updateProcedure(EMAIL, NONEXISTENT_ID, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("deve lancar excecao quando novo titulo ja existe no hospital")
        void shouldThrowWhenNewTitleDuplicated() {
            ProcedureRequest renameRequest = new ProcedureRequest("Cirurgia de mao", null, 90, null);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(procedureRepository.existsByHospitalIdAndTitleIgnoreCase(HOSPITAL_ID, "Cirurgia de mao"))
                    .thenReturn(true);

            assertThatThrownBy(() -> procedureService.updateProcedure(EMAIL, PROCEDURE_ID, renameRequest))
                    .isInstanceOf(BusinessException.class);

            verify(procedureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deactivateProcedure")
    class DeactivateProcedure {

        @Test
        @DisplayName("deve inativar procedimento sem remover o registro")
        void shouldDeactivateProcedure() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));

            procedureService.deactivateProcedure(EMAIL, PROCEDURE_ID);

            assertThat(procedure.getActive()).isFalse();
            verify(procedureRepository).save(procedure);
            verify(procedureRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando procedimento ja esta inativo")
        void shouldThrowWhenAlreadyInactive() {
            procedure.setActive(false);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));

            assertThatThrownBy(() -> procedureService.deactivateProcedure(EMAIL, PROCEDURE_ID))
                    .isInstanceOf(BusinessException.class);

            verify(procedureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("assignDoctor")
    class AssignDoctor {

        private final DoctorProcedureRequest assignRequest = new DoctorProcedureRequest(DOCTOR_ID);

        @Test
        @DisplayName("deve atrelar medico ao procedimento")
        void shouldAssignDoctorSuccessfully() {
            DoctorProcedure link = new DoctorProcedure(doctor, procedure);
            DoctorProcedureResponse linkResponse = new DoctorProcedureResponse(
                    UUID.randomUUID(), null, null, null);

            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(false);
            when(doctorProcedureMapper.toEntity(doctor, procedure)).thenReturn(link);
            when(doctorProcedureRepository.save(link)).thenReturn(link);
            when(doctorProcedureMapper.toResponse(link)).thenReturn(linkResponse);

            DoctorProcedureResponse result = procedureService.assignDoctor(EMAIL, PROCEDURE_ID, assignRequest);

            assertThat(result).isEqualTo(linkResponse);
            verify(doctorProcedureRepository).save(link);
        }

        @Test
        @DisplayName("deve lancar excecao quando medico e de outro hospital")
        void shouldThrowWhenDoctorBelongsToAnotherHospital() {
            doctor.setHospital(otherHospital);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));

            assertThatThrownBy(() -> procedureService.assignDoctor(EMAIL, PROCEDURE_ID, assignRequest))
                    .isInstanceOf(UnauthorizedException.class);

            verify(doctorProcedureRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando medico ja esta atrelado")
        void shouldThrowWhenAlreadyAssigned() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
            when(doctorProcedureRepository.existsByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> procedureService.assignDoctor(EMAIL, PROCEDURE_ID, assignRequest))
                    .isInstanceOf(BusinessException.class);

            verify(doctorProcedureRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("unassignDoctor")
    class UnassignDoctor {

        @Test
        @DisplayName("deve desatrelar medico do procedimento")
        void shouldUnassignDoctorSuccessfully() {
            DoctorProcedure link = new DoctorProcedure(doctor, procedure);
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
            when(doctorProcedureRepository.findByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(Optional.of(link));

            procedureService.unassignDoctor(EMAIL, PROCEDURE_ID, DOCTOR_ID);

            verify(doctorProcedureRepository).delete(link);
        }

        @Test
        @DisplayName("deve lancar excecao quando medico nao esta atrelado")
        void shouldThrowWhenNotAssigned() {
            when(userRepository.findByEmailWithHospital(EMAIL)).thenReturn(Optional.of(hospitalUser));
            when(procedureRepository.findById(PROCEDURE_ID)).thenReturn(Optional.of(procedure));
            when(doctorRepository.findById(DOCTOR_ID)).thenReturn(Optional.of(doctor));
            when(doctorProcedureRepository.findByDoctorIdAndProcedureId(DOCTOR_ID, PROCEDURE_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> procedureService.unassignDoctor(EMAIL, PROCEDURE_ID, DOCTOR_ID))
                    .isInstanceOf(BusinessException.class);

            verify(doctorProcedureRepository, never()).delete(any());
        }
    }
}
