package com.tcc.application.dto.response;

import com.tcc.domain.model.CheckinSource;
import java.time.LocalDateTime;
import java.util.UUID;

public record CheckinResponse(UUID id, UUID patientProcedureId, CheckinSource source,
                              LocalDateTime submittedAt) {
}
