package com.tcc.application.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.mapper.HospitalMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class HospitalServiceImplTest {

    @Mock
    private HospitalRepository hospitalRepository;

    @Mock
    private HospitalMapper hospitalMapper;

    @InjectMocks
    private HospitalServiceImpl hospitalService;

    private Hospital hospital;
    private HospitalRequest request;
    private HospitalResponse response;

    private static final UUID HOSPITAL_ID = UUID.randomUUID();
    private static final UUID NONEXISTENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setId(HOSPITAL_ID);

        request = new HospitalRequest("Hospital Central", "12345678000100", "1133334444", "contato@hospital.com", "Rua B", "Sao Paulo", "SP");

        response = new HospitalResponse(HOSPITAL_ID, "Hospital Central", "12345678000100", "1133334444", "contato@hospital.com", "Rua B", "Sao Paulo", "SP", true, null, null);
    }

    @Nested
    @DisplayName("createHospital")
    class CreateHospital {

        @Test
        @DisplayName("deve criar hospital com sucesso")
        void shouldCreateHospitalSuccessfully() {
            when(hospitalRepository.existsByCnpj("12345678000100")).thenReturn(false);
            when(hospitalMapper.toEntity(request)).thenReturn(hospital);
            when(hospitalRepository.save(hospital)).thenReturn(hospital);
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            HospitalResponse result = hospitalService.createHospital(request);

            assertThat(result).isEqualTo(response);
            verify(hospitalRepository).save(hospital);
        }

        @Test
        @DisplayName("deve lancar excecao quando CNPJ duplicado")
        void shouldThrowWhenDuplicateCnpj() {
            when(hospitalRepository.existsByCnpj("12345678000100")).thenReturn(true);

            assertThatThrownBy(() -> hospitalService.createHospital(request))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("CNPJ");

            verify(hospitalRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteHospital")
    class DeleteHospital {

        @Test
        @DisplayName("deve inativar hospital sem doutores associados, sem remover do banco")
        void shouldInactivateHospitalWithoutDoctors() {
            hospital.setDoctors(new ArrayList<>());
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));

            hospitalService.deleteHospital(HOSPITAL_ID);

            assertThat(hospital.getActive()).isFalse();
            verify(hospitalRepository).save(hospital);
            verify(hospitalRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando ha doutores associados")
        void shouldThrowWhenHasAssociatedDoctors() {
            List<Doctor> doctors = List.of(new Doctor());
            hospital.setDoctors(new ArrayList<>(doctors));
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));

            assertThatThrownBy(() -> hospitalService.deleteHospital(HOSPITAL_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("doutores associados");

            assertThat(hospital.getActive()).isTrue();
            verify(hospitalRepository, never()).save(any());
            verify(hospitalRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deve lancar excecao quando hospital nao encontrado")
        void shouldThrowWhenHospitalNotFound() {
            when(hospitalRepository.findById(NONEXISTENT_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> hospitalService.deleteHospital(NONEXISTENT_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getHospitalsByActive")
    class GetHospitalsByActive {

        @Test
        @DisplayName("deve consultar o repository filtrando por active true")
        void shouldQueryRepositoryWithActiveTrue() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hospitalRepository.findByActive(true, pageable))
                    .thenReturn(new PageImpl<>(List.of(hospital)));
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            Page<HospitalResponse> result = hospitalService.getHospitalsByActive(true, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(hospitalRepository).findByActive(true, pageable);
            verify(hospitalRepository, never()).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("deve consultar o repository filtrando por active false")
        void shouldQueryRepositoryWithActiveFalse() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hospitalRepository.findByActive(false, pageable))
                    .thenReturn(new PageImpl<>(List.of()));

            Page<HospitalResponse> result = hospitalService.getHospitalsByActive(false, pageable);

            assertThat(result.getContent()).isEmpty();
            verify(hospitalRepository).findByActive(false, pageable);
            verify(hospitalRepository, never()).findAll(any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("filterHospitals com status")
    class FilterHospitalsWithActive {

        @Test
        @DisplayName("deve delegar para busca por status quando so o active e informado")
        void shouldDelegateToActiveSearchWhenOnlyActiveIsProvided() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hospitalRepository.findByActive(true, pageable))
                    .thenReturn(new PageImpl<>(List.of(hospital)));
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            Page<HospitalResponse> result = hospitalService.filterHospitals(null, null, null, true, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(hospitalRepository).findByActive(true, pageable);
        }

        @Test
        @DisplayName("deve combinar filtros textuais com o status no banco")
        void shouldCombineTextFiltersWithActive() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hospitalRepository.findByFiltersAndActive("Central", null, null, false, pageable))
                    .thenReturn(new PageImpl<>(List.of(hospital)));
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            Page<HospitalResponse> result = hospitalService.filterHospitals("Central", null, null, false, pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(hospitalRepository).findByFiltersAndActive("Central", null, null, false, pageable);
        }

        @Test
        @DisplayName("deve usar filtro sem status quando active e nulo")
        void shouldUseFilterWithoutActiveWhenActiveIsNull() {
            Pageable pageable = PageRequest.of(0, 10);
            when(hospitalRepository.findByFilters("Central", null, null, pageable))
                    .thenReturn(new PageImpl<>(List.of(hospital)));
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            hospitalService.filterHospitals("Central", null, null, null, pageable);

            verify(hospitalRepository).findByFilters("Central", null, null, pageable);
            verify(hospitalRepository, never()).findByActive(any(), any());
        }
    }

    @Nested
    @DisplayName("getAllHospitals")
    class GetAllHospitals {

        @Test
        @DisplayName("deve buscar todos os hospitais sem aplicar filtro de status")
        void shouldFetchAllWithoutActiveFilter() {
            Pageable pageable = PageRequest.of(0, 200);
            when(hospitalRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(hospital)));
            when(hospitalMapper.toResponse(hospital)).thenReturn(response);

            Page<HospitalResponse> result = hospitalService.getAllHospitals(pageable);

            assertThat(result.getContent()).containsExactly(response);
            verify(hospitalRepository).findAll(pageable);
            verify(hospitalRepository, never()).findByActive(any(), any());
        }
    }

    @Nested
    @DisplayName("getHospitalStats")
    class GetHospitalStats {

        @Test
        @DisplayName("deve retornar as contagens vindas do repository")
        void shouldReturnCountsFromRepository() {
            when(hospitalRepository.countByActiveTrue()).thenReturn(7L);
            when(hospitalRepository.countByActiveFalse()).thenReturn(3L);

            Map<String, Object> stats = hospitalService.getHospitalStats();

            assertThat(stats.get("totalHospitals")).isEqualTo(10L);
            assertThat(stats.get("activeHospitals")).isEqualTo(7L);
            assertThat(stats.get("inactiveHospitals")).isEqualTo(3L);
            assertThat(stats.get("lastUpdate")).isInstanceOf(LocalDateTime.class);
        }

        @Test
        @DisplayName("deve retornar zero para inativos quando todos estao ativos")
        void shouldReturnZeroInactiveWhenAllActive() {
            when(hospitalRepository.countByActiveTrue()).thenReturn(4L);
            when(hospitalRepository.countByActiveFalse()).thenReturn(0L);

            Map<String, Object> stats = hospitalService.getHospitalStats();

            assertThat(stats.get("totalHospitals")).isEqualTo(4L);
            assertThat(stats.get("activeHospitals")).isEqualTo(4L);
            assertThat(stats.get("inactiveHospitals")).isEqualTo(0L);
        }
    }
}
