package com.tcc.application.service;

import com.tcc.application.dto.request.ManualCheckinRequest;
import com.tcc.application.dto.request.AggregatedManualCheckinRequest;
import com.tcc.application.dto.response.AggregatedCheckinResponse;
import com.tcc.application.dto.response.CheckinResponse;
import java.util.UUID;

public interface CheckinService {
    CheckinResponse submitManual(String email, UUID patientProcedureId, ManualCheckinRequest request);

    AggregatedCheckinResponse submitAggregatedManual(String email, AggregatedManualCheckinRequest request);
}
