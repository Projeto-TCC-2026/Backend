package com.tcc.application.service;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;

import java.util.UUID;

public interface DashboardService {
    
    AdminDashboardResponse getAdminDashboard();
    
    HospitalDashboardResponse getHospitalDashboard(UUID hospitalId, String requesterEmail);
}
