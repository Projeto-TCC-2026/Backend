package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final DoctorMapper doctorMapper;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             HospitalRepository hospitalRepository,
                             UserRepository userRepository,
                             DoctorMapper doctorMapper) {
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.doctorMapper = doctorMapper;
    }

    @Override
    @Transactional
    public DoctorResponse createDoctor(DoctorRequest request) {
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(request.userId())));

        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(request.hospitalId())));

        if (doctorRepository.existsByCpf(request.cpf())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCpf(request.cpf()));
        }

        if (doctorRepository.existsByCrm(request.crm())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCrm(request.crm()));
        }

        if (doctorRepository.findByUserId(request.userId()).isPresent()) {
            throw new BusinessException(ErrorMessages.userAlreadyAssociatedWithDoctor());
        }

        Doctor doctor = doctorMapper.toEntity(request, user, hospital);
        Doctor savedDoctor = doctorRepository.save(doctor);
        
        return doctorMapper.toResponse(savedDoctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAll(pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(id)));
        
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctor(UUID id, DoctorRequest request) {
        Doctor existingDoctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(id)));

        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(request.hospitalId())));

        if (!existingDoctor.getCpf().equals(request.cpf()) && 
            doctorRepository.existsByCpf(request.cpf())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCpf(request.cpf()));
        }

        if (!existingDoctor.getCrm().equals(request.crm()) && 
            doctorRepository.existsByCrm(request.crm())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCrm(request.crm()));
        }

        doctorMapper.updateEntity(existingDoctor, request, hospital);
        Doctor updatedDoctor = doctorRepository.save(existingDoctor);
        
        return doctorMapper.toResponse(updatedDoctor);
    }

    @Override
    @Transactional
    public void deleteDoctor(UUID id) {
        Doctor doctor = doctorRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(id)));

        if (!doctor.getDoctorPatients().isEmpty()) {
            throw new BusinessException(ErrorMessages.doctorHasPatients(doctor.getDoctorPatients().size()));
        }

        if (!doctor.getProcedures().isEmpty()) {
            throw new BusinessException(ErrorMessages.doctorHasProcedures(doctor.getProcedures().size()));
        }

        doctorRepository.delete(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> searchByName(String name, Pageable pageable) {
        return doctorRepository.findByFullNameContainingIgnoreCase(name, pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse searchByCrm(String crm) {
        Doctor doctor = doctorRepository.findByCrm(crm)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundByCrm(crm)));
        
        return doctorMapper.toResponse(doctor);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> searchBySpecialty(String specialty, Pageable pageable) {
        return doctorRepository.findBySpecialtyContainingIgnoreCase(specialty, pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> filterDoctors(UUID hospitalId, String specialty, String name, String crm, Pageable pageable) {
        return doctorRepository.findByFilters(hospitalId, specialty, name, crm, pageable)
                .map(doctorMapper::toResponse);
    }
}
