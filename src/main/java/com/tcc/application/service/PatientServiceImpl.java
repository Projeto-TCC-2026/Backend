package com.tcc.application.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.ProcedureExecutionResponse;
import com.tcc.application.mapper.PatientMapper;
import com.tcc.application.mapper.ProcedureExecutionMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorPatient;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureExecutionRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class PatientServiceImpl implements PatientService {

    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final ProcedureExecutionRepository procedureExecutionRepository;
    private final PatientMapper patientMapper;
    private final ProcedureExecutionMapper procedureExecutionMapper;

    public PatientServiceImpl(PatientRepository patientRepository,
                             UserRepository userRepository,
                             DoctorRepository doctorRepository,
                             DoctorPatientRepository doctorPatientRepository,
                             ProcedureExecutionRepository procedureExecutionRepository,
                             PatientMapper patientMapper,
                             ProcedureExecutionMapper procedureExecutionMapper) {
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.procedureExecutionRepository = procedureExecutionRepository;
        this.patientMapper = patientMapper;
        this.procedureExecutionMapper = procedureExecutionMapper;
    }

    @Override
    @Transactional
    public PatientResponse createPatient(String email, PatientRequest request) {
        Doctor doctor = resolveDoctor(email);

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(request.userId())));

        if (patientRepository.existsByCpfAndActiveTrue(request.cpf())) {
            throw new BusinessException(ErrorMessages.duplicateActivePatientCpf(request.cpf()));
        }

        if (patientRepository.findByUserId(request.userId()).isPresent()) {
            throw new BusinessException(ErrorMessages.userAlreadyAssociatedWithPatient());
        }

        if (request.email() != null && !request.email().trim().isEmpty()) {
            if (patientRepository.existsByEmailAndActiveTrue(request.email())) {
                throw new BusinessException(ErrorMessages.duplicateActivePatientEmail(request.email()));
            }
        }

        Patient patient = patientMapper.toEntity(request, user);
        Patient savedPatient = patientRepository.save(patient);

        doctorPatientRepository.save(new DoctorPatient(doctor, savedPatient));

        return patientMapper.toResponse(savedPatient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllActivePatients(String requesterEmail, Pageable pageable) {
        User requester = findRequester(requesterEmail);
        return listVisible(requester, null, null, null, null, null, null, null, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponse getPatientById(String requesterEmail, UUID id) {
        Patient patient = findAccessiblePatient(requesterEmail, id);
        return patientMapper.toResponse(patient);
    }

    @Override
    @Transactional
    public PatientResponse updatePatient(String requesterEmail, UUID id, PatientRequest request) {
        Patient existingPatient = findAccessiblePatient(requesterEmail, id);

        if (!existingPatient.getCpf().equals(request.cpf()) &&
            patientRepository.existsByCpfAndActiveTrue(request.cpf())) {
            throw new BusinessException(ErrorMessages.duplicateActivePatientCpf(request.cpf()));
        }

        if (request.email() != null && !request.email().trim().isEmpty()) {
            if (existingPatient.getEmail() == null || !existingPatient.getEmail().equals(request.email())) {
                if (patientRepository.existsByEmailAndActiveTrue(request.email())) {
                    throw new BusinessException(ErrorMessages.duplicateActivePatientEmail(request.email()));
                }
            }
        }

        if (!existingPatient.getUser().getId().equals(request.userId())) {
            User newUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundById(request.userId())));

            if (patientRepository.findByUserId(request.userId()).isPresent()) {
                throw new BusinessException(ErrorMessages.userAlreadyAssociatedWithPatient());
            }

            existingPatient.setUser(newUser);
        }

        patientMapper.updateEntity(existingPatient, request);
        Patient updatedPatient = patientRepository.save(existingPatient);

        return patientMapper.toResponse(updatedPatient);
    }

    @Override
    @Transactional
    public void deletePatient(UUID id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.patientNotFoundById(id)));

        if (patient.hasProcedureExecutions()) {
            throw new BusinessException(ErrorMessages.patientHasProcedureExecutions(
                    patient.countProcedureExecutions()));
        }

        if (!patient.getHealthReadings().isEmpty()) {
            throw new BusinessException(ErrorMessages.PATIENT_HAS_HEALTH_READINGS);
        }

        patientRepository.delete(patient);
    }

    @Override
    @Transactional
    public void inactivatePatient(String requesterEmail, UUID id) {
        Patient patient = findAccessiblePatient(requesterEmail, id);
        patient.inactivate();
        patientRepository.save(patient);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByName(String requesterEmail, String name, Pageable pageable) {
        return listVisible(findRequester(requesterEmail), name, null, null, null, null, null, null, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByCpf(String requesterEmail, String cpf, Pageable pageable) {
        return listVisible(findRequester(requesterEmail), null, cpf, null, null, null, null, null, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByEmail(String requesterEmail, String email, Pageable pageable) {
        return listVisible(findRequester(requesterEmail), null, null, email, null, null, null, null, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> searchByPhone(String requesterEmail, String phone, Pageable pageable) {
        return listVisible(findRequester(requesterEmail), null, null, null, phone, null, null, null, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientResponse> filterPatients(String requesterEmail, String name, String gender, String city, String state, Pageable pageable) {
        return listVisible(findRequester(requesterEmail), name, null, null, null, gender, city, state, pageable)
                .map(patientMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureExecutionResponse> getPatientProcedureExecutions(String requesterEmail, UUID patientId, Pageable pageable) {
        findAccessiblePatient(requesterEmail, patientId);

        return procedureExecutionRepository.findPagedByPatientId(patientId, pageable)
                .map(procedureExecutionMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Long countPatientProcedureExecutions(String requesterEmail, UUID patientId) {
        findAccessiblePatient(requesterEmail, patientId);
        return procedureExecutionRepository.countByPatientId(patientId);
    }

    @Override
    @Transactional(readOnly = true)
    public long countAllPatients() {
        return patientRepository.count();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActivePatients() {
        return patientRepository.countByActiveTrue();
    }

    private Page<Patient> listVisible(User requester,
                                      String name,
                                      String cpf,
                                      String email,
                                      String phone,
                                      String gender,
                                      String city,
                                      String state,
                                      Pageable pageable) {
        UUID doctorId = null;
        UUID hospitalId = null;

        if (requester.getRole() == Role.DOCTOR) {
            doctorId = resolveDoctor(requester.getEmail()).getId();
        } else if (requester.getRole() == Role.HOSPITAL) {
            hospitalId = requireHospitalId(requester);
        } else if (requester.getRole() != Role.ADMIN) {
            throw new UnauthorizedException("Perfil sem permissão para listar pacientes");
        }

        boolean noFilters = name == null && cpf == null && email == null && phone == null
                && gender == null && city == null && state == null;

        if (noFilters && requester.getRole() == Role.ADMIN) {
            return patientRepository.findPagedByActiveTrue(pageable);
        }
        if (noFilters && requester.getRole() == Role.DOCTOR) {
            return patientRepository.findPagedActiveByDoctorId(doctorId, pageable);
        }
        if (noFilters && requester.getRole() == Role.HOSPITAL) {
            return patientRepository.findPagedActiveByHospitalId(hospitalId, pageable);
        }

        return patientRepository.findVisible(
                doctorId, hospitalId, name, cpf, email, phone, gender, city, state, pageable);
    }

    private Patient findAccessiblePatient(String requesterEmail, UUID id) {
        Patient patient = patientRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.patientNotFoundById(id)));

        assertCanAccess(findRequester(requesterEmail), patient.getId());
        return patient;
    }

    private void assertCanAccess(User requester, UUID patientId) {
        switch (requester.getRole()) {
            case ADMIN -> { }
            case DOCTOR -> {
                Doctor doctor = resolveDoctor(requester.getEmail());
                if (!doctorPatientRepository.existsByDoctorIdAndPatientId(doctor.getId(), patientId)) {
                    throw new UnauthorizedException(ErrorMessages.patientNotLinkedToDoctor());
                }
            }
            case HOSPITAL -> {
                UUID hospitalId = requireHospitalId(requester);
                if (!doctorPatientRepository.existsByDoctor_Hospital_IdAndPatient_Id(hospitalId, patientId)) {
                    throw new UnauthorizedException(ErrorMessages.patientNotInHospital());
                }
            }
            default -> throw new UnauthorizedException("Perfil sem permissão para acessar pacientes");
        }
    }

    private User findRequester(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));
    }

    private UUID requireHospitalId(User requester) {
        if (requester.getHospital() == null) {
            throw new UnauthorizedException(ErrorMessages.hospitalProfileNotFound());
        }
        return requester.getHospital().getId();
    }

    private Doctor resolveDoctor(String email) {
        User doctorUser = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));

        return doctorRepository.findByUserId(doctorUser.getId())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.doctorProfileNotFound()));
    }
}
