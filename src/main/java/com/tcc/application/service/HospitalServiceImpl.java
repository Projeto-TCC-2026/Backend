package com.tcc.application.service;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.mapper.HospitalMapper;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        // Validar CNPJ único
        if (hospitalRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessException("Já existe um hospital cadastrado com o CNPJ: " + request.cnpj());
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
    public HospitalResponse getHospitalById(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + id));
        
        return hospitalMapper.toResponse(hospital);
    }

    @Override
    @Transactional
    public HospitalResponse updateHospital(Long id, HospitalRequest request) {
        Hospital existingHospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + id));

        // Validar CNPJ único (exceto o próprio hospital)
        if (!existingHospital.getCnpj().equals(request.cnpj()) && 
            hospitalRepository.existsByCnpj(request.cnpj())) {
            throw new BusinessException("Já existe um hospital cadastrado com o CNPJ: " + request.cnpj());
        }

        hospitalMapper.updateEntity(existingHospital, request);
        Hospital updatedHospital = hospitalRepository.save(existingHospital);
        
        return hospitalMapper.toResponse(updatedHospital);
    }

    @Override
    @Transactional
    public void deleteHospital(Long id) {
        Hospital hospital = hospitalRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hospital não encontrado com ID: " + id));

        // Verificar se há doutores associados
        if (!hospital.getDoctors().isEmpty()) {
            throw new BusinessException("Não é possível excluir o hospital. Existem " + 
                    hospital.getDoctors().size() + " doutores associados.");
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
}
