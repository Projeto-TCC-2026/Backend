package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.PatientProcedureRequest;
import com.tcc.application.dto.response.PatientProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.mapper.PatientProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.repository.DoctorPatientRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class PatientProcedureServiceImpl implements PatientProcedureService {

    private final PatientProcedureRepository patientProcedureRepository;
    private final ProcedureRepository procedureRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final DoctorPatientRepository doctorPatientRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final PatientProcedureMapper patientProcedureMapper;
    private final ProcedureMapper procedureMapper;

    public PatientProcedureServiceImpl(PatientProcedureRepository patientProcedureRepository,
                                       ProcedureRepository procedureRepository,
                                       DoctorProcedureRepository doctorProcedureRepository,
                                       DoctorPatientRepository doctorPatientRepository,
                                       DoctorRepository doctorRepository,
                                       PatientRepository patientRepository,
                                       UserRepository userRepository,
                                       PatientProcedureMapper patientProcedureMapper,
                                       ProcedureMapper procedureMapper) {
        this.patientProcedureRepository = patientProcedureRepository;
        this.procedureRepository = procedureRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.doctorPatientRepository = doctorPatientRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.patientProcedureMapper = patientProcedureMapper;
        this.procedureMapper = procedureMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponse> listAvailableProcedures(String email) {
        Doctor doctor = resolveDoctor(email);

        return procedureRepository.findActiveByDoctorId(doctor.getId()).stream()
                .map(procedureMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PatientProcedureResponse assignProcedure(String email, UUID patientId,
                                                    PatientProcedureRequest request) {
        Doctor doctor = resolveDoctor(email);
        Patient patient = findPatientLinkedToDoctor(patientId, doctor.getId());
        Procedure procedure = findProcedureAssignedToDoctor(request.procedureId(), doctor.getId());

        if (patientProcedureRepository.existsByPatientIdAndProcedureIdAndDoctorId(
                patient.getId(), procedure.getId(), doctor.getId())) {
            throw new BusinessException(ErrorMessages.duplicatePatientProcedure());
        }

        PatientProcedure patientProcedure =
                patientProcedureMapper.toEntity(request, patient, procedure, doctor);
        PatientProcedure saved = patientProcedureRepository.save(patientProcedure);

        return patientProcedureMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PatientProcedureResponse> listPatientProcedures(String email, UUID patientId,
                                                                Pageable pageable) {
        Doctor doctor = resolveDoctor(email);
        findPatientLinkedToDoctor(patientId, doctor.getId());

        return patientProcedureRepository
                .findByPatientIdAndDoctorId(patientId, doctor.getId(), pageable)
                .map(patientProcedureMapper::toResponse);
    }

    @Override
    @Transactional
    public PatientProcedureResponse updateAssignment(String email, UUID patientId, UUID assignmentId,
                                                     PatientProcedureRequest request) {
        Doctor doctor = resolveDoctor(email);
        findPatientLinkedToDoctor(patientId, doctor.getId());
        PatientProcedure patientProcedure = findOwnAssignment(assignmentId, patientId, doctor.getId());

        patientProcedureMapper.updateEntity(patientProcedure, request);
        PatientProcedure updated = patientProcedureRepository.save(patientProcedure);

        return patientProcedureMapper.toResponse(updated);
    }

    @Override
    @Transactional
    public void removeAssignment(String email, UUID patientId, UUID assignmentId) {
        Doctor doctor = resolveDoctor(email);
        findPatientLinkedToDoctor(patientId, doctor.getId());
        PatientProcedure patientProcedure = findOwnAssignment(assignmentId, patientId, doctor.getId());

        patientProcedureRepository.delete(patientProcedure);
    }

    // --- Helpers ---

    private Doctor resolveDoctor(String email) {
        var user = userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));

        return doctorRepository.findByUserId(user.getId())
                .orElseThrow(() -> new UnauthorizedException(ErrorMessages.doctorProfileNotFound()));
    }

    private Patient findPatientLinkedToDoctor(UUID patientId, UUID doctorId) {
        Patient patient = patientRepository.findByIdAndActiveTrue(patientId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.patientNotFoundById(patientId)));

        if (!doctorPatientRepository.existsByDoctorIdAndPatientId(doctorId, patient.getId())) {
            throw new UnauthorizedException(ErrorMessages.patientNotLinkedToDoctor());
        }

        return patient;
    }

    /**
     * Garante que o médico só atribua procedimento que o hospital autorizou para ele
     * e que ainda esteja ativo. Filtrar apenas a listagem não protegeria a escrita.
     */
    private Procedure findProcedureAssignedToDoctor(UUID procedureId, UUID doctorId) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.procedureNotFoundById(procedureId)));

        if (!doctorProcedureRepository.existsByDoctorIdAndProcedureId(doctorId, procedure.getId())) {
            throw new UnauthorizedException(ErrorMessages.procedureNotAssignedToDoctor());
        }

        if (!Boolean.TRUE.equals(procedure.getActive())) {
            throw new BusinessException(ErrorMessages.procedureInactiveForAssignment());
        }

        return procedure;
    }

    private PatientProcedure findOwnAssignment(UUID assignmentId, UUID patientId, UUID doctorId) {
        return patientProcedureRepository
                .findByIdAndPatientIdAndDoctorId(assignmentId, patientId, doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.patientProcedureNotFoundById(assignmentId)));
    }
}
