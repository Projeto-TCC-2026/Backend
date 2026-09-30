package com.tcc.application.service;

import com.tcc.application.dto.response.PublicStatsResponse;

public interface PublicStatsService {

    /**
     * Retorna as contagens totais de pacientes, médicos e hospitais,
     * sem filtro de ativo ou status.
     */
    PublicStatsResponse getPublicStats();
}
