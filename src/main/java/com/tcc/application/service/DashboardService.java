package com.tcc.application.service;

import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.dto.response.HospitalDashboardResponse;

public interface DashboardService {
    
    AdminDashboardResponse getAdminDashboard();
    
    HospitalDashboardResponse getHospitalDashboard(Long hospitalId, String requesterEmail);
}
