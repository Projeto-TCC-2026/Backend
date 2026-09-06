package com.tcc.application.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record AggregatedCheckinResponse(
        UUID submissionId,
        List<UUID> checkinIds,
        LocalDateTime submittedAt,
        LocalDateTime editableUntil) {
}
