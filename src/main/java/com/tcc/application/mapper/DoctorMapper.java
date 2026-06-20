package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.DoctorSummary;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.User;

@Component
public class DoctorMapper {

    private final UserMapper userMapper;
    private final HospitalMapper hospitalMapper;

    public DoctorMapper(UserMapper userMapper, HospitalMapper hospitalMapper) {
        this.userMapper = userMapper;
        this.hospitalMapper = hospitalMapper;
    }

    public DoctorResponse toResponse(Doctor doctor) {
        if (doctor == null) return null;
        return new DoctorResponse(
                doctor.getId(),
                userMapper.toResponse(doctor.getUser()),
                hospitalMapper.toSummary(doctor.getHospital()),
                doctor.getFullName(),
                doctor.getCpf(),
                doctor.getCrm(),
                doctor.getSpecialty(),
                doctor.getPhone(),
                doctor.getCreatedAt(),
                doctor.getUpdatedAt()
        );
    }

    public DoctorSummary toSummary(Doctor doctor) {
        if (doctor == null) return null;
        return new DoctorSummary(
                doctor.getId(),
                doctor.getFullName(),
                doctor.getCrm(),
                doctor.getSpecialty()
        );
    }

    public Doctor toEntity(DoctorRequest request, User user, Hospital hospital) {
        if (request == null) return null;
        Doctor doctor = new Doctor();
        doctor.setUser(user);
        doctor.setHospital(hospital);
        doctor.setFullName(request.fullName());
        doctor.setCpf(request.cpf());
        doctor.setCrm(request.crm());
        doctor.setSpecialty(request.specialty());
        doctor.setPhone(request.phone());
        return doctor;
    }

    public void updateEntity(Doctor doctor, DoctorRequest request, Hospital hospital) {
        doctor.setHospital(hospital);
        doctor.setFullName(request.fullName());
        doctor.setCpf(request.cpf());
        doctor.setCrm(request.crm());
        doctor.setSpecialty(request.specialty());
        doctor.setPhone(request.phone());
    }
}
