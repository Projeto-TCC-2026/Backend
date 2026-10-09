package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;

@ExtendWith(MockitoExtension.class)
class AccountAccessServiceTest {

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private PatientRepository patientRepository;

    @InjectMocks
    private AccountAccessService accountAccessService;

    @Test
    void rejectsDoctorWhenHospitalIsInactive() {
        User user = new User("doctor@test.com", "hash", Role.DOCTOR);
        user.setId(UUID.randomUUID());
        Hospital hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setActive(false);
        Doctor doctor = new Doctor(user, hospital, "Dr. Carlos", "11122233344", "CRM12345");

        when(doctorRepository.findByUserId(user.getId())).thenReturn(Optional.of(doctor));

        assertThat(accountAccessService.isAccountActive(user)).isFalse();
    }

    @Test
    void rejectsInactiveDoctorProfileEvenWhenUserIsActive() {
        User user = new User("doctor@test.com", "hash", Role.DOCTOR);
        user.setId(UUID.randomUUID());
        Hospital hospital = new Hospital("Hospital Central", "12345678000100");
        Doctor doctor = new Doctor(user, hospital, "Dr. Carlos", "11122233344", "CRM12345");
        doctor.setActive(false);

        when(doctorRepository.findByUserId(user.getId())).thenReturn(Optional.of(doctor));

        assertThat(accountAccessService.isAccountActive(user)).isFalse();
    }

    @Test
    void rejectsInactivePatientProfileEvenWhenUserIsActive() {
        User user = new User("patient@test.com", "hash", Role.PATIENT);
        user.setId(UUID.randomUUID());
        Patient patient = new Patient();
        patient.setActive(false);

        when(patientRepository.findByUserId(user.getId())).thenReturn(Optional.of(patient));

        assertThat(accountAccessService.isAccountActive(user)).isFalse();
    }

    @Test
    void rejectsInactiveHospitalEvenWhenUserIsActive() {
        User user = new User("hospital@test.com", "hash", Role.HOSPITAL);
        Hospital hospital = new Hospital("Hospital Central", "12345678000100");
        hospital.setActive(false);
        user.setHospital(hospital);

        assertThat(accountAccessService.isAccountActive(user)).isFalse();
    }
}
