package com.tcc.application.service;

import com.tcc.application.dto.request.DoctorRequest;
import com.tcc.application.dto.response.DoctorResponse;
import com.tcc.application.dto.response.DoctorSummary;

import java.util.List;

public interface DoctorService {

    DoctorResponse create(DoctorRequest request);

    List<DoctorSummary> findAll();

    DoctorResponse findById(Long id);

    DoctorResponse update(Long id, DoctorRequest request);

    void inactive(Long id);
}
