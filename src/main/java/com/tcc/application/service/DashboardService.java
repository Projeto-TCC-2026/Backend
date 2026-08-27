package com.tcc.application.service;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.DoctorDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;

import java.util.UUID;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    HospitalDashboardResponse getHospitalDashboard(UUID hospitalId, String requesterEmail);

    HospitalDashboardResponse getHospitalDashboardForDoctor(String doctorEmail);

    DoctorDashboardResponse getDoctorDashboard(String doctorEmail);
}
