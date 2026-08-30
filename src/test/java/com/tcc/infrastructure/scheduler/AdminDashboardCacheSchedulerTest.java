package com.tcc.infrastructure.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.application.dto.response.AdminDashboardResponse;
import com.tcc.application.service.DashboardService;
import com.tcc.infrastructure.storage.JsonCacheStorage;

@ExtendWith(MockitoExtension.class)
class AdminDashboardCacheSchedulerTest {

    private static final String CACHE_KEY = "dashboard/admin.json";

    @Mock
    private DashboardService dashboardService;

    @Mock
    private JsonCacheStorage jsonCacheStorage;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AdminDashboardCacheScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new AdminDashboardCacheScheduler(
                dashboardService,
                jsonCacheStorage,
                objectMapper,
                CACHE_KEY);
    }

    @Test
    @DisplayName("grava no cache o JSON do dashboard calculado no banco")
    void shouldWriteCalculatedDashboardAsJson() throws Exception {
        AdminDashboardResponse dashboard = new AdminDashboardResponse(
                10L, 8L, 2L, 30L, 25L, 5L, 400L);

        when(dashboardService.getAdminDashboard()).thenReturn(dashboard);

        scheduler.refreshAdminDashboardCache();

        ArgumentCaptor<String> jsonCaptor = ArgumentCaptor.forClass(String.class);
        verify(jsonCacheStorage).write(eq(CACHE_KEY), jsonCaptor.capture());

        assertThat(objectMapper.readValue(
                jsonCaptor.getValue(), AdminDashboardResponse.class))
                .isEqualTo(dashboard);
    }
}
