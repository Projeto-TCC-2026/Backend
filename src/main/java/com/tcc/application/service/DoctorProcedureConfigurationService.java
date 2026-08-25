package com.tcc.application.service;

import java.util.List;
import java.util.UUID;

import com.tcc.application.dto.request.DoctorProcedureFieldRequest;
import com.tcc.application.dto.response.CheckinFormResponse;
import com.tcc.application.dto.response.DoctorProcedureFieldResponse;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.FieldTypePresetResponse;

public interface DoctorProcedureConfigurationService {
  List<FieldTypePresetResponse> listFieldTypePresets();

  List<DoctorProcedureResponse> listOwnProcedures(String email);

  List<DoctorProcedureFieldResponse> listFields(String email, UUID doctorProcedureId);

  List<DoctorProcedureFieldResponse> replaceFields(String email, UUID doctorProcedureId,
      List<DoctorProcedureFieldRequest> requests);

  CheckinFormResponse getPatientCheckinForm(String email, UUID patientProcedureId);
}
