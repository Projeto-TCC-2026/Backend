package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.application.dto.response.PublicStatsResponse;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class PublicStatsServiceImplTest {

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private HospitalRepository hospitalRepository;

    @InjectMocks
    private PublicStatsServiceImpl publicStatsService;

    @Nested
    @DisplayName("getPublicStats")
    class GetPublicStats {

        @Test
        @DisplayName("deve mapear cada contagem para o campo correspondente do DTO")
        void shouldMapEachCountToItsField() {
            when(patientRepository.count()).thenReturn(120L);
            when(doctorRepository.count()).thenReturn(35L);
            when(hospitalRepository.count()).thenReturn(8L);

            PublicStatsResponse result = publicStatsService.getPublicStats();

            assertThat(result.patients()).isEqualTo(120L);
            assertThat(result.doctors()).isEqualTo(35L);
            assertThat(result.hospitals()).isEqualTo(8L);
        }

        @Test
        @DisplayName("deve retornar zero quando não há registros")
        void shouldReturnZeroWhenThereAreNoRecords() {
            when(patientRepository.count()).thenReturn(0L);
            when(doctorRepository.count()).thenReturn(0L);
            when(hospitalRepository.count()).thenReturn(0L);

            PublicStatsResponse result = publicStatsService.getPublicStats();

            assertThat(result.patients()).isZero();
            assertThat(result.doctors()).isZero();
            assertThat(result.hospitals()).isZero();
        }

        @Test
        @DisplayName("deve contar via count, sem carregar registros com findAll")
        void shouldCountWithoutLoadingRecords() {
            when(patientRepository.count()).thenReturn(1L);
            when(doctorRepository.count()).thenReturn(2L);
            when(hospitalRepository.count()).thenReturn(3L);

            publicStatsService.getPublicStats();

            verify(patientRepository).count();
            verify(doctorRepository).count();
            verify(hospitalRepository).count();
            verify(patientRepository, never()).findAll();
            verify(doctorRepository, never()).findAll();
            verify(hospitalRepository, never()).findAll();
        }
    }
}
