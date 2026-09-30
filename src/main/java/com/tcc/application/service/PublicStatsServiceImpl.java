package com.tcc.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.response.PublicStatsResponse;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.PatientRepository;

@Service
public class PublicStatsServiceImpl implements PublicStatsService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;

    public PublicStatsServiceImpl(PatientRepository patientRepository,
                                  DoctorRepository doctorRepository,
                                  HospitalRepository hospitalRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PublicStatsResponse getPublicStats() {
        // count() do Spring Data: COUNT no banco, sem carregar entidade.
        // Conta todos os registros, sem filtro de ativo ou status.
        long patients = patientRepository.count();
        long doctors = doctorRepository.count();
        long hospitals = hospitalRepository.count();

        return new PublicStatsResponse(patients, doctors, hospitals);
    }
}
