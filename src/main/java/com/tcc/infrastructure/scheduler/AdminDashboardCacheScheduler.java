package com.tcc.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.service.DashboardService;
import com.tcc.infrastructure.storage.JsonCacheStorage;

@Component
public class AdminDashboardCacheScheduler {

    private static final Logger log = LoggerFactory.getLogger(
            AdminDashboardCacheScheduler.class);

    private final DashboardService dashboardService;
    private final JsonCacheStorage jsonCacheStorage;
    private final ObjectMapper objectMapper;
    private final String adminCacheKey;

    public AdminDashboardCacheScheduler(
            DashboardService dashboardService,
            JsonCacheStorage jsonCacheStorage,
            ObjectMapper objectMapper,
            @Value("${app.dashboard-cache.admin-key}") String adminCacheKey) {
        this.dashboardService = dashboardService;
        this.jsonCacheStorage = jsonCacheStorage;
        this.objectMapper = objectMapper;
        this.adminCacheKey = adminCacheKey;
    }

    @Scheduled(
            fixedDelayString = "${app.dashboard-cache.refresh-interval-ms}",
            initialDelayString = "${app.dashboard-cache.initial-delay-ms:300000}")
    public void refreshAdminDashboardCache() {
        try {
            AdminDashboardResponse dashboard =
                    dashboardService.getAdminDashboard();

            jsonCacheStorage.write(
                    adminCacheKey,
                    objectMapper.writeValueAsString(dashboard));

        } catch (JsonProcessingException e) {
            log.error(
                    "Erro ao serializar dashboard admin para o cache. key={}, exception={}",
                    adminCacheKey,
                    e.getClass().getSimpleName());

        } catch (Exception e) {
            log.error(
                    "Erro ao atualizar o cache do dashboard admin. key={}, exception={}",
                    adminCacheKey,
                    e.getClass().getSimpleName());
        }
    }
}
