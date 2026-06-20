package com.tcc.application.mapper;

import org.springframework.stereotype.Component;

import com.tcc.application.dto.request.HospitalRequest;
import com.tcc.application.dto.response.HospitalResponse;
import com.tcc.application.dto.response.HospitalSummary;
import com.tcc.domain.model.Hospital;

@Component
public class HospitalMapper {

    public HospitalResponse toResponse(Hospital hospital) {
        if (hospital == null) return null;
        return new HospitalResponse(
                hospital.getId(),
                hospital.getName(),
                hospital.getCnpj(),
                hospital.getPhone(),
                hospital.getEmail(),
                hospital.getAddress(),
                hospital.getCity(),
                hospital.getState(),
                hospital.getCreatedAt(),
                hospital.getUpdatedAt()
        );
    }

    public HospitalSummary toSummary(Hospital hospital) {
        if (hospital == null) return null;
        return new HospitalSummary(
                hospital.getId(),
                hospital.getName(),
                hospital.getCity(),
                hospital.getState()
        );
    }

    public Hospital toEntity(HospitalRequest request) {
        if (request == null) return null;
        Hospital hospital = new Hospital();
        hospital.setName(request.name());
        hospital.setCnpj(request.cnpj());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());
        hospital.setAddress(request.address());
        hospital.setCity(request.city());
        hospital.setState(request.state());
        return hospital;
    }

    public void updateEntity(Hospital hospital, HospitalRequest request) {
        hospital.setName(request.name());
        hospital.setCnpj(request.cnpj());
        hospital.setPhone(request.phone());
        hospital.setEmail(request.email());
        hospital.setAddress(request.address());
        hospital.setCity(request.city());
        hospital.setState(request.state());
    }
}
