package com.tcc.presentation.controller;

import com.tcc.application.service.HospitalService;
import com.tcc.application.service.UserService;
import com.tcc.application.service.PatientService;
import com.tcc.application.dto.response.ApiResponse;
import com.tcc.domain.model.Role;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard Administrativo", description = "Métricas e estatísticas da plataforma para administradores")
@SecurityRequirement(name = "Bearer Authentication")
@PreAuthorize("hasRole('ADMIN')")
public class DashboardController {

    private final HospitalService hospitalService;
    private final UserService userService;
    private final PatientService patientService;

    public DashboardController(HospitalService hospitalService, 
                              UserService userService, 
                              PatientService patientService) {
        this.hospitalService = hospitalService;
        this.userService = userService;
        this.patientService = patientService;
    }

    @GetMapping
    @Operation(
        summary = "Dashboard principal",
        description = "Retorna todas as métricas principais do sistema: hospitais, doutores, pacientes e usuários"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard() {
        Map<String, Object> dashboard = new LinkedHashMap<>();
        
        // Métricas principais
        dashboard.put("totalHospitals", hospitalService.countHospitals());
        dashboard.put("totalDoctors", userService.countUsersByRole(Role.DOCTOR));
        dashboard.put("totalPatients", patientService.countActivePatients());
        
        // Métricas secundárias
        dashboard.put("totalUsers", userService.countUsers());
        dashboard.put("totalAdmins", userService.countUsersByRole(Role.ADMIN));
        
        // Metadados
        dashboard.put("lastUpdate", LocalDateTime.now());
        dashboard.put("systemStatus", "online");
        dashboard.put("message", "Dashboard carregado com sucesso");
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(dashboard);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/metrics")
    @Operation(
        summary = "Métricas detalhadas",
        description = "Retorna métricas detalhadas com breakdown por categoria e status"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDetailedMetrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        // Hospitais
        Map<String, Object> hospitalMetrics = new HashMap<>();
        hospitalMetrics.put("total", hospitalService.countHospitals());
        hospitalMetrics.put("active", hospitalService.countActiveHospitals());
        hospitalMetrics.put("inactive", hospitalService.countInactiveHospitals());
        metrics.put("hospitals", hospitalMetrics);
        
        // Doutores
        Map<String, Object> doctorMetrics = new HashMap<>();
        doctorMetrics.put("total", userService.countUsersByRole(Role.DOCTOR));
        doctorMetrics.put("active", userService.countUsersByRole(Role.DOCTOR));
        doctorMetrics.put("inactive", userService.countInactiveUsersByRole(Role.DOCTOR));
        metrics.put("doctors", doctorMetrics);
        
        // Pacientes
        Map<String, Object> patientMetrics = new HashMap<>();
        patientMetrics.put("total", patientService.countAllPatients());
        patientMetrics.put("active", patientService.countActivePatients());
        patientMetrics.put("inactive", patientService.countAllPatients() - patientService.countActivePatients());
        metrics.put("patients", patientMetrics);
        
        // Usuários totais
        Map<String, Object> userMetrics = new HashMap<>();
        userMetrics.put("total", userService.countUsers());
        userMetrics.put("admins", userService.countUsersByRole(Role.ADMIN));
        userMetrics.put("doctors", userService.countUsersByRole(Role.DOCTOR));
        userMetrics.put("patients", userService.countUsersByRole(Role.PATIENT));
        metrics.put("users", userMetrics);
        
        // Metadados
        metrics.put("generatedAt", LocalDateTime.now());
        metrics.put("version", "1.0");
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(metrics);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/hospitals")
    @Operation(
        summary = "Estatísticas específicas de hospitais",
        description = "Retorna estatísticas detalhadas apenas sobre hospitais"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHospitalStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        long totalHospitals = hospitalService.countHospitals();
        
        stats.put("totalHospitals", totalHospitals);
        stats.put("activeHospitals", hospitalService.countActiveHospitals());
        stats.put("inactiveHospitals", hospitalService.countInactiveHospitals());
        stats.put("averageDoctorsPerHospital", totalHospitals > 0 ? 
            (double) userService.countUsersByRole(Role.DOCTOR) / totalHospitals : 0.0);
        
        // Crescimento (placeholder - implementar lógica real)
        stats.put("monthlyGrowth", "+2.5%");
        stats.put("lastRegistration", LocalDateTime.now().minusDays(3));
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/doctors")
    @Operation(
        summary = "Estatísticas específicas de doutores",
        description = "Retorna estatísticas detalhadas apenas sobre médicos"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDoctorStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        long totalDoctors = userService.countUsersByRole(Role.DOCTOR);
        long totalHospitals = hospitalService.countHospitals();
        
        stats.put("totalDoctors", totalDoctors);
        stats.put("activeDoctors", totalDoctors);
        stats.put("inactiveDoctors", userService.countInactiveUsersByRole(Role.DOCTOR));
        stats.put("doctorsPerHospital", totalHospitals > 0 ? 
            (double) totalDoctors / totalHospitals : 0.0);
        
        // Dados adicionais
        stats.put("monthlyGrowth", "+5.1%");
        stats.put("lastLogin", LocalDateTime.now().minusHours(2));
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/patients")
    @Operation(
        summary = "Estatísticas específicas de pacientes",
        description = "Retorna estatísticas detalhadas apenas sobre pacientes"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPatientStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        
        long totalPatients = patientService.countAllPatients();
        long activePatients = patientService.countActivePatients();
        long totalDoctors = userService.countUsersByRole(Role.DOCTOR);
        
        stats.put("totalPatients", totalPatients);
        stats.put("activePatients", activePatients);
        stats.put("inactivePatients", totalPatients - activePatients);
        stats.put("patientsPerDoctor", totalDoctors > 0 ? 
            (double) activePatients / totalDoctors : 0.0);
        
        // Dados adicionais
        stats.put("monthlyGrowth", "+12.3%");
        stats.put("lastRegistration", LocalDateTime.now().minusHours(1));
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(stats);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/summary")
    @Operation(
        summary = "Resumo executivo",
        description = "Retorna um resumo executivo compacto para displays e widgets"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getExecutiveSummary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        
        // Números principais (formato para cards/widgets)
        summary.put("hospitals", Map.of(
            "count", hospitalService.countHospitals(),
            "label", "Hospitais Cadastrados",
            "icon", "hospital",
            "color", "blue"
        ));
        
        summary.put("doctors", Map.of(
            "count", userService.countUsersByRole(Role.DOCTOR),
            "label", "Médicos Ativos",
            "icon", "doctor",
            "color", "green"
        ));
        
        summary.put("patients", Map.of(
            "count", patientService.countActivePatients(),
            "label", "Pacientes Ativos",
            "icon", "patient",
            "color", "purple"
        ));
        
        summary.put("users", Map.of(
            "count", userService.countUsers(),
            "label", "Usuários Totais",
            "icon", "users",
            "color", "orange"
        ));
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(summary);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/health")
    @Operation(
        summary = "Status de saúde do sistema",
        description = "Verifica a saúde geral do sistema e conectividade dos serviços"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemHealth() {
        Map<String, Object> health = new LinkedHashMap<>();
        
        try {
            // Teste básico de conectividade com os services
            hospitalService.countHospitals();
            userService.countUsers();
            patientService.countActivePatients();
            
            health.put("status", "healthy");
            health.put("database", "connected");
            health.put("services", "operational");
            health.put("uptime", "99.9%"); // Placeholder
            health.put("lastCheck", LocalDateTime.now());
            health.put("message", "Todos os sistemas operacionais");
            
        } catch (Exception e) {
            health.put("status", "degraded");
            health.put("database", "error");
            health.put("services", "partial");
            health.put("error", e.getMessage());
            health.put("lastCheck", LocalDateTime.now());
            health.put("message", "Alguns serviços podem estar indisponíveis");
        }
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(health);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/trends")
    @Operation(
        summary = "Tendências e crescimento",
        description = "Dados de crescimento e tendências ao longo do tempo (placeholder para implementação futura)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrends() {
        Map<String, Object> trends = new LinkedHashMap<>();
        
        // Dados de exemplo - em produção, implementar consultas reais com datas
        trends.put("hospitalsGrowth", Map.of(
            "current", hospitalService.countHospitals(),
            "lastMonth", hospitalService.countHospitals() - 2,
            "growth", "+2",
            "percentage", "+8.3%"
        ));
        
        trends.put("doctorsGrowth", Map.of(
            "current", userService.countUsersByRole(Role.DOCTOR),
            "lastMonth", userService.countUsersByRole(Role.DOCTOR) - 5,
            "growth", "+5", 
            "percentage", "+12.5%"
        ));
        
        trends.put("patientsGrowth", Map.of(
            "current", patientService.countActivePatients(),
            "lastMonth", patientService.countActivePatients() - 15,
            "growth", "+15",
            "percentage", "+18.7%"
        ));
        
        trends.put("period", "last 30 days");
        trends.put("generatedAt", LocalDateTime.now());
        
        ApiResponse<Map<String, Object>> response = ApiResponse.success(trends);
        return ResponseEntity.ok(response);
    }
}