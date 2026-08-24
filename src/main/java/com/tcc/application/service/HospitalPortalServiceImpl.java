package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRegistrationRequest;
import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.DoctorRegistrationResponse;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.mapper.DoctorMapper;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HospitalPortalServiceImpl implements HospitalPortalService {

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final HospitalService hospitalService;
    private final DoctorService doctorService;
    private final DashboardService dashboardService;
    private final DoctorMapper doctorMapper;

    public HospitalPortalServiceImpl(UserRepository userRepository,
                                     DoctorRepository doctorRepository,
                                     HospitalService hospitalService,
                                     DoctorService doctorService,
                                     DashboardService dashboardService,
                                     DoctorMapper doctorMapper) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.hospitalService = hospitalService;
        this.doctorService = doctorService;
        this.dashboardService = dashboardService;
        this.doctorMapper = doctorMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalResponse getOwnHospital(String email) {
        Hospital hospital = resolveHospital(email);
        return hospitalService.getHospitalById(hospital.getId());
    }

    @Override
    @Transactional
    public HospitalResponse updateOwnHospital(String email, HospitalRequest request) {
        Hospital hospital = resolveHospital(email);
        return hospitalService.updateHospital(hospital.getId(), request);
    }

    @Override
    @Transactional(readOnly = true)
    public HospitalDashboardResponse getDashboard(String email) {
        Hospital hospital = resolveHospital(email);
        return dashboardService.getHospitalDashboard(hospital.getId(), email);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DoctorResponse> listDoctors(String email, Pageable pageable) {
        Hospital hospital = resolveHospital(email);
        return doctorRepository.findByHospitalIdAndActiveTrue(hospital.getId(), pageable)
                .map(doctorMapper::toResponse);
    }

    @Override
    @Transactional
    public DoctorResponse createDoctor(String email, DoctorRequest request) {
        Hospital hospital = resolveHospital(email);

        if (!hospital.getId().equals(request.hospitalId())) {
            throw new BusinessException("O hospital informado não corresponde ao seu hospital");
        }

        return doctorService.createDoctor(request);
    }

    @Override
    @Transactional
    public DoctorRegistrationResponse registerDoctor(String email, DoctorRegistrationRequest request) {
        Hospital hospital = resolveHospital(email);

        if (!hospital.getId().equals(request.hospitalId())) {
            throw new BusinessException("O hospital informado não corresponde ao seu hospital");
        }

        return doctorService.registerDoctor(request);
    }

    @Override
    @Transactional(readOnly = true)
    public DoctorResponse getDoctorById(String email, UUID doctorId) {
        Hospital hospital = resolveHospital(email);
        validateDoctorBelongsToHospital(doctorId, hospital.getId());
        return doctorService.getDoctorById(doctorId);
    }

    @Override
    @Transactional
    public DoctorResponse updateDoctor(String email, UUID doctorId, DoctorRequest request) {
        Hospital hospital = resolveHospital(email);
        validateDoctorBelongsToHospital(doctorId, hospital.getId());

        if (!hospital.getId().equals(request.hospitalId())) {
            throw new BusinessException("O hospital informado não corresponde ao seu hospital");
        }

        return doctorService.updateDoctor(doctorId, request);
    }

    @Override
    @Transactional
    public void deleteDoctor(String email, UUID doctorId) {
        Hospital hospital = resolveHospital(email);
        validateDoctorBelongsToHospital(doctorId, hospital.getId());
        doctorService.deleteDoctor(doctorId);
    }

    // --- Helpers ---

    private Hospital resolveHospital(String email) {
        User user = userRepository.findByEmailWithHospital(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getHospital() == null) {
            throw new UnauthorizedException("Usuário não está vinculado a nenhum hospital");
        }

        return user.getHospital();
    }

    private void validateDoctorBelongsToHospital(UUID doctorId, UUID hospitalId) {
        var doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.doctorNotFoundById(doctorId)));

        if (!doctor.getHospital().getId().equals(hospitalId)) {
            throw new UnauthorizedException("Este médico não pertence ao seu hospital");
        }
    }
}
