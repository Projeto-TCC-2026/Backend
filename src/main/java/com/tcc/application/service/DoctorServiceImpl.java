package com.tcc.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.DoctorRegistrationRequest;
import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.AccessLinkResponse;
import com.tcc.application.dto.response.DoctorRegistrationResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final HospitalRepository hospitalRepository;
    private final UserRepository userRepository;
    private final DoctorMapper doctorMapper;
    private final PasswordEncoder passwordEncoder;
    private final AccountActivationService accountActivationService;

    public DoctorServiceImpl(DoctorRepository doctorRepository,
                             HospitalRepository hospitalRepository,
                             UserRepository userRepository,
                             DoctorMapper doctorMapper,
                             PasswordEncoder passwordEncoder,
                             AccountActivationService accountActivationService) {
        this.doctorRepository = doctorRepository;
        this.hospitalRepository = hospitalRepository;
        this.userRepository = userRepository;
        this.doctorMapper = doctorMapper;
        this.passwordEncoder = passwordEncoder;
        this.accountActivationService = accountActivationService;
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
    @Transactional
    public DoctorRegistrationResponse registerDoctor(DoctorRegistrationRequest request) {
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(request.hospitalId())));

        User existingUser = userRepository.findByEmail(request.email()).orElse(null);
        if (existingUser != null) {
            if (Boolean.TRUE.equals(existingUser.getActive()) || existingUser.getDoctor() == null) {
                throw new BusinessException(ErrorMessages.duplicateUserEmail(request.email()));
            }

            Doctor existingDoctor = existingUser.getDoctor();
            existingDoctor.setActive(true);
            existingDoctor.setHospital(hospital);
            existingDoctor.setFullName(request.fullName());
            existingDoctor.setCpf(request.cpf());
            existingDoctor.setCrm(request.crm());
            existingDoctor.setSpecialty(request.specialty());
            existingDoctor.setPhone(request.phone());
            existingUser.setActive(true);
            userRepository.save(existingUser);
            Doctor savedDoctor = doctorRepository.save(existingDoctor);
            String activationLink = accountActivationService.issueActivationToken(existingUser, request.fullName());
            return new DoctorRegistrationResponse(doctorMapper.toResponse(savedDoctor), activationLink);
        }

        if (doctorRepository.existsByCpf(request.cpf())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCpf(request.cpf()));
        }

        if (doctorRepository.existsByCrm(request.crm())) {
            throw new BusinessException(ErrorMessages.duplicateDoctorCrm(request.crm()));
        }

        // Senha temporária aleatória: ninguém a conhece, o doutor só entra depois de
        // definir a própria senha através do link de boas-vindas enviado por e-mail.
        String temporaryPassword = UUID.randomUUID().toString() + UUID.randomUUID();
        User user = new User(request.email(), passwordEncoder.encode(temporaryPassword), Role.DOCTOR);
        User savedUser = userRepository.save(user);

        Doctor doctor = new Doctor();
        doctor.setUser(savedUser);
        doctor.setHospital(hospital);
        doctor.setFullName(request.fullName());
        doctor.setCpf(request.cpf());
        doctor.setCrm(request.crm());
        doctor.setSpecialty(request.specialty());
        doctor.setPhone(request.phone());
        Doctor savedDoctor = doctorRepository.save(doctor);

        String activationLink = accountActivationService.issueActivationToken(savedUser, request.fullName());

        return new DoctorRegistrationResponse(doctorMapper.toResponse(savedDoctor), activationLink);
    }

    @Override
    @Transactional
    public AccessLinkResponse generateAccessLink(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(doctorId)));

        String activationLink = accountActivationService.issueActivationToken(doctor.getUser(), doctor.getFullName());

        return new AccessLinkResponse(activationLink);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> getAllDoctors(Pageable pageable) {
        return doctorRepository.findAllByActiveTrue(pageable)
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

        if (!doctor.getDoctorProcedures().isEmpty()) {
            throw new BusinessException(ErrorMessages.doctorHasProcedures(doctor.getDoctorProcedures().size()));
        }

        doctor.setActive(false);
        doctorRepository.save(doctor);
        if (doctor.getUser() != null) {
            doctor.getUser().setActive(false);
            userRepository.save(doctor.getUser());
        }
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
