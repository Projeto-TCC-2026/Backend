package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.HospitalSummary;
import com.tcc.application.mapper.HospitalMapper;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HospitalServiceImpl implements HospitalService {

    private final HospitalRepository hospitalRepository;
    private final HospitalMapper hospitalMapper;

    public HospitalServiceImpl(HospitalRepository hospitalRepository, 
                               HospitalMapper hospitalMapper) {
        this.hospitalRepository = hospitalRepository;
        this.hospitalMapper = hospitalMapper;
    }

    @Override
    @Transactional
    public HospitalResponse createHospital(HospitalRequest request) {
        if (hospitalRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessException(ErrorMessages.duplicateHospitalCnpj(request.cnpj()));
        }

        Hospital hospital = hospitalMapper.toEntity(request);
        Hospital savedHospital = hospitalRepository.save(hospital);
        
        return hospitalMapper.toResponse(savedHospital);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> getAllHospitals(Pageable pageable) {
        return hospitalRepository.findAll(pageable)
                .map(hospitalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse getHospitalById(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(id)));
        
        return hospitalMapper.toResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(UUID id, HospitalRequest request) {
        Hospital existingHospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(id)));

        if (!existingHospital.getCnpj().equals(request.cnpj()) && 
            hospitalRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessException(ErrorMessages.duplicateHospitalCnpj(request.cnpj()));
        }

        hospitalMapper.updateEntity(existingHospital, request);
        Hospital updatedHospital = hospitalRepository.save(existingHospital);
        
        return hospitalMapper.toResponse(updatedHospital);
    }

    @Override
    @Transactional
    public void deleteHospital(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(id)));

        if (!hospital.getDoctors().isEmpty()) {
            throw new BusinessException(ErrorMessages.hospitalHasDoctors(hospital.getDoctors().size()));
        }

        hospitalRepository.delete(hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> searchByName(String name, Pageable pageable) {
        return hospitalRepository.findByNameContainingIgnoreCase(name, pageable)
                .map(hospitalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalResponse> filterHospitals(String name, String city, String state, Pageable pageable) {
        return hospitalRepository.findByFilters(name, city, state, pageable)
                .map(hospitalMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public long countHospitals() {
        return hospitalRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveHospitals() {
        return hospitalRepository.countByActiveTrue();
    }

    @Override
    @Transactional(readOnly = true)
    public long countInactiveHospitals() {
        return hospitalRepository.countByActiveFalse();
    }

    @Override
    @Transactional
    public void enableHospital(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(id)));
        
        hospital.setActive(true);
        hospitalRepository.save(hospital);
    }

    @Override
    @Transactional
    public void disableHospital(UUID id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(id)));
        
        hospital.setActive(false);
        hospitalRepository.save(hospital);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<HospitalSummary> getHospitalsSummary(Pageable pageable) {
        return hospitalRepository.findHospitalsSummary(pageable);
    }
}
