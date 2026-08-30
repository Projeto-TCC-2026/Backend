package com.tcc.application.service;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.DoctorDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;

import java.util.UUID;

public interface DashboardService {

    AdminDashboardResponse getAdminDashboard();

    /**
     * Devolve o dashboard administrativo a partir do cache. Se o cache não
     * existir ou estiver ilegível, calcula no banco.
     */
    AdminDashboardResponse getAdminDashboardCached();

    HospitalDashboardResponse getHospitalDashboard(UUID hospitalId, String requesterEmail);

    HospitalDashboardResponse getHospitalDashboardForDoctor(String doctorEmail);

    DoctorDashboardResponse getDoctorDashboard(String doctorEmail);
}
