package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.mapper.HospitalMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.repository.HospitalRepository;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

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
        @DisplayName("deve excluir hospital sem doutores associados")
        void shouldDeleteHospitalWithoutDoctors() {
            hospital.setDoctors(new ArrayList<>());
            when(hospitalRepository.findById(HOSPITAL_ID)).thenReturn(Optional.of(hospital));

            hospitalService.deleteHospital(HOSPITAL_ID);

            verify(hospitalRepository).delete(hospital);
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
}
