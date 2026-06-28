package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.PatientRequest;
import com.tcc.application.dto.response.PatientResponse;
import com.tcc.application.dto.response.PatientSummary;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.User;

@Component
public class PatientMapper {

    private final UserMapper userMapper;

    public PatientMapper(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public PatientResponse toResponse(Patient patient) {
        if (patient == null) return null;
        return new PatientResponse(
                patient.getId(),
                userMapper.toResponse(patient.getUser()),
                patient.getFullName(),
                patient.getCpf(),
                patient.getBirthDate(),
                patient.getGender(),
                patient.getPhone(),
                patient.getWeight(),
                patient.getHeight(),
                patient.getActive(),
                patient.getCreatedAt(),
                patient.getUpdatedAt()
        );
    }

    public PatientSummary toSummary(Patient patient) {
        if (patient == null) return null;
        return new PatientSummary(
                patient.getId(),
                patient.getFullName(),
                patient.getBirthDate()
        );
    }

    public Patient toEntity(PatientRequest request, User user) {
        if (request == null) return null;
        Patient patient = new Patient();
        patient.setUser(user);
        patient.setFullName(request.fullName());
        patient.setCpf(request.cpf());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setWeight(request.weight());
        patient.setHeight(request.height());
        return patient;
    }

    public void updateEntity(Patient patient, PatientRequest request) {
        patient.setFullName(request.fullName());
        patient.setCpf(request.cpf());
        patient.setBirthDate(request.birthDate());
        patient.setGender(request.gender());
        patient.setPhone(request.phone());
        patient.setWeight(request.weight());
        patient.setHeight(request.height());
    }
}
