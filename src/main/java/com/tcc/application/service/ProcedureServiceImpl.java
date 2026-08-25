package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.DoctorProcedureRequest;
import com.tcc.application.dto.request.ProcedureRequest;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.ProcedureResponse;
import com.tcc.application.mapper.DoctorProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HospitalRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class ProcedureServiceImpl implements ProcedureService {

    private final ProcedureRepository procedureRepository;
    private final HospitalRepository hospitalRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final ProcedureMapper procedureMapper;
    private final DoctorProcedureMapper doctorProcedureMapper;

    public ProcedureServiceImpl(ProcedureRepository procedureRepository,
                                HospitalRepository hospitalRepository,
                                DoctorProcedureRepository doctorProcedureRepository,
                                DoctorRepository doctorRepository,
                                UserRepository userRepository,
                                ProcedureMapper procedureMapper,
                                DoctorProcedureMapper doctorProcedureMapper) {
        this.procedureRepository = procedureRepository;
        this.hospitalRepository = hospitalRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.procedureMapper = procedureMapper;
        this.doctorProcedureMapper = doctorProcedureMapper;
    }

    @Override
    @Transactional
    public ProcedureResponse createProcedure(String email, ProcedureRequest request) {
        Hospital hospital = resolveHospital(email);

        return createProcedure(hospital, request);
    }

    @Override
    @Transactional
    public ProcedureResponse createProcedureForHospital(UUID hospitalId, ProcedureRequest request) {
        Hospital hospital = findHospital(hospitalId);
        return createProcedure(hospital, request);
    }

    private ProcedureResponse createProcedure(Hospital hospital, ProcedureRequest request) {

        if (procedureRepository.existsByHospitalIdAndTitleIgnoreCase(hospital.getId(), request.title())) {
            throw new BusinessException(ErrorMessages.duplicateProcedureTitle(request.title()));
        }

        Procedure procedure = procedureMapper.toEntity(request, hospital);
        Procedure savedProcedure = procedureRepository.save(procedure);

        return procedureMapper.toResponse(savedProcedure);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureResponse> listProcedures(String email, Boolean includeInactive, Pageable pageable) {
        Hospital hospital = resolveHospital(email);

        return listProceduresForKnownHospital(hospital, includeInactive, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProcedureResponse> listProceduresForHospital(UUID hospitalId, Boolean includeInactive, Pageable pageable) {
        return listProceduresForKnownHospital(findHospital(hospitalId), includeInactive, pageable);
    }

    private Page<ProcedureResponse> listProceduresForKnownHospital(Hospital hospital, Boolean includeInactive, Pageable pageable) {

        Page<Procedure> procedures = Boolean.TRUE.equals(includeInactive)
                ? procedureRepository.findByHospitalId(hospital.getId(), pageable)
                : procedureRepository.findByHospitalIdAndActive(hospital.getId(), true, pageable);

        return procedures.map(procedureMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponse getProcedureById(String email, UUID id) {
        Hospital hospital = resolveHospital(email);
        Procedure procedure = findProcedureInHospital(id, hospital.getId());

        return procedureMapper.toResponse(procedure);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponse getProcedureByIdForHospital(UUID hospitalId, UUID id) {
        return procedureMapper.toResponse(findProcedureInHospital(id, findHospital(hospitalId).getId()));
    }

    @Override
    @Transactional
    public ProcedureResponse updateProcedure(String email, UUID id, ProcedureRequest request) {
        Hospital hospital = resolveHospital(email);

        return updateProcedure(hospital, id, request);
    }

    @Override
    @Transactional
    public ProcedureResponse updateProcedureForHospital(UUID hospitalId, UUID id, ProcedureRequest request) {
        return updateProcedure(findHospital(hospitalId), id, request);
    }

    private ProcedureResponse updateProcedure(Hospital hospital, UUID id, ProcedureRequest request) {
        Procedure procedure = findProcedureInHospital(id, hospital.getId());

        if (!procedure.getTitle().equalsIgnoreCase(request.title())
                && procedureRepository.existsByHospitalIdAndTitleIgnoreCase(hospital.getId(), request.title())) {
            throw new BusinessException(ErrorMessages.duplicateProcedureTitle(request.title()));
        }

        procedureMapper.updateEntity(procedure, request);
        Procedure updatedProcedure = procedureRepository.save(procedure);

        return procedureMapper.toResponse(updatedProcedure);
    }

    @Override
    @Transactional
    public void deactivateProcedure(String email, UUID id) {
        Hospital hospital = resolveHospital(email);

        deactivateProcedureForKnownHospital(hospital, id);
    }

    @Override
    @Transactional
    public void deactivateProcedureForHospital(UUID hospitalId, UUID id) {
        deactivateProcedureForKnownHospital(findHospital(hospitalId), id);
    }

    private void deactivateProcedureForKnownHospital(Hospital hospital, UUID id) {
        Procedure procedure = findProcedureInHospital(id, hospital.getId());

        if (!Boolean.TRUE.equals(procedure.getActive())) {
            throw new BusinessException(ErrorMessages.procedureAlreadyInactive());
        }

        procedure.setActive(false);
        procedureRepository.save(procedure);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorProcedureResponse> listProcedureDoctors(String email, UUID procedureId) {
        Hospital hospital = resolveHospital(email);
        findProcedureInHospital(procedureId, hospital.getId());

        return doctorProcedureRepository.findByProcedureId(procedureId).stream()
                .map(doctorProcedureMapper::toResponse)
                .toList();
    }

            @Override
            @Transactional(readOnly = true)
            public List<DoctorProcedureResponse> listProcedureDoctorsForAdmin(UUID procedureId) {
            procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.procedureNotFoundById(procedureId)));
            return doctorProcedureRepository.findByProcedureId(procedureId).stream()
                .map(doctorProcedureMapper::toResponse)
                .toList();
            }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorProcedureResponse> listDoctorProcedures(String email, UUID doctorId) {
        Hospital hospital = resolveHospital(email);
        return listDoctorProceduresForHospital(doctorId, hospital.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorProcedureResponse> listDoctorProceduresForAdmin(UUID doctorId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(doctorId)));
        return doctorProcedureRepository.findByDoctorId(doctor.getId()).stream()
                .map(doctorProcedureMapper::toResponse)
                .toList();
    }

    private List<DoctorProcedureResponse> listDoctorProceduresForHospital(UUID doctorId, UUID hospitalId) {
        Doctor doctor = findDoctorInHospital(doctorId, hospitalId);
        return doctorProcedureRepository.findByDoctorId(doctor.getId()).stream()
                .map(doctorProcedureMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public DoctorProcedureResponse assignDoctor(String email, UUID procedureId, DoctorProcedureRequest request) {
        Hospital hospital = resolveHospital(email);
        return assignDoctor(procedureId, request, hospital.getId());
    }

    @Override
    @Transactional
    public DoctorProcedureResponse assignDoctorForAdmin(UUID procedureId, DoctorProcedureRequest request) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.procedureNotFoundById(procedureId)));
        return assignDoctor(procedureId, request, procedure.getHospital().getId());
    }

    private DoctorProcedureResponse assignDoctor(UUID procedureId, DoctorProcedureRequest request, UUID hospitalId) {
        Procedure procedure = findProcedureInHospital(procedureId, hospitalId);
        Doctor doctor = findDoctorInHospital(request.doctorId(), hospitalId);

        if (doctorProcedureRepository.existsByDoctorIdAndProcedureId(doctor.getId(), procedure.getId())) {
            throw new BusinessException(ErrorMessages.doctorAlreadyAssignedToProcedure());
        }

        DoctorProcedure doctorProcedure = doctorProcedureMapper.toEntity(doctor, procedure);
        DoctorProcedure savedDoctorProcedure = doctorProcedureRepository.save(doctorProcedure);

        return doctorProcedureMapper.toResponse(savedDoctorProcedure);
    }

    @Override
    @Transactional
    public void unassignDoctor(String email, UUID procedureId, UUID doctorId) {
        Hospital hospital = resolveHospital(email);
        unassignDoctor(procedureId, doctorId, hospital.getId());
    }

    @Override
    @Transactional
    public void unassignDoctorForAdmin(UUID procedureId, UUID doctorId) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.procedureNotFoundById(procedureId)));
        unassignDoctor(procedureId, doctorId, procedure.getHospital().getId());
    }

    private void unassignDoctor(UUID procedureId, UUID doctorId, UUID hospitalId) {
        Procedure procedure = findProcedureInHospital(procedureId, hospitalId);
        Doctor doctor = findDoctorInHospital(doctorId, hospitalId);

        DoctorProcedure doctorProcedure = doctorProcedureRepository
                .findByDoctorIdAndProcedureId(doctor.getId(), procedure.getId())
                .orElseThrow(() -> new BusinessException(ErrorMessages.doctorNotAssignedToProcedure()));

        doctorProcedureRepository.delete(doctorProcedure);
    }

    // --- Helpers ---

    private Hospital resolveHospital(String email) {
        User user = userRepository.findByEmailWithHospital(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)));

        if (user.getHospital() == null) {
            throw new UnauthorizedException("Usuário não está vinculado a nenhum hospital");
        }

        return user.getHospital();
    }

    private Hospital findHospital(UUID hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.hospitalNotFoundById(hospitalId)));
    }

    private Procedure findProcedureInHospital(UUID procedureId, UUID hospitalId) {
        Procedure procedure = procedureRepository.findById(procedureId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.procedureNotFoundById(procedureId)));

        if (!procedure.getHospital().getId().equals(hospitalId)) {
            throw new UnauthorizedException(ErrorMessages.procedureNotInHospital());
        }

        return procedure;
    }

    private Doctor findDoctorInHospital(UUID doctorId, UUID hospitalId) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(doctorId)));

        if (!doctor.getHospital().getId().equals(hospitalId)) {
            throw new UnauthorizedException(ErrorMessages.doctorNotInHospital());
        }

        return doctor;
    }
}
