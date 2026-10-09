package com.tcc.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;

@Service
public class AccountAccessService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;

    public AccountAccessService(DoctorRepository doctorRepository, PatientRepository patientRepository) {
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public boolean isAccountActive(User user) {
        if (!Boolean.TRUE.equals(user.getActive())) {
            return false;
        }

        return switch (user.getRole()) {
            case ADMIN -> true;
            case DOCTOR -> doctorRepository.findByUserId(user.getId())
                    .map(doctor -> Boolean.TRUE.equals(doctor.getActive())
                            && doctor.getHospital() != null
                            && Boolean.TRUE.equals(doctor.getHospital().getActive()))
                    .orElse(false);
            case PATIENT -> patientRepository.findByUserId(user.getId())
                    .map(patient -> Boolean.TRUE.equals(patient.getActive()))
                    .orElse(false);
            case HOSPITAL -> user.getHospital() != null
                    && Boolean.TRUE.equals(user.getHospital().getActive());
        };
    }
}
