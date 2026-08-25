package com.tcc.application.service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.tcc.application.dto.request.DoctorProcedureFieldRequest;
import com.tcc.application.dto.response.CheckinFormResponse;
import com.tcc.application.dto.response.DoctorProcedureFieldResponse;
import com.tcc.application.dto.response.DoctorProcedureResponse;
import com.tcc.application.dto.response.FieldTypePresetResponse;
import com.tcc.application.mapper.DoctorProcedureMapper;
import com.tcc.application.mapper.ProcedureMapper;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldThreshold;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.domain.repository.FieldTypePresetRepository;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;

@Service
public class DoctorProcedureConfigurationServiceImpl implements DoctorProcedureConfigurationService {
  private final UserRepository userRepository;
  private final DoctorRepository doctorRepository;
  private final DoctorProcedureRepository doctorProcedureRepository;
  private final DoctorProcedureFieldRepository fieldRepository;
  private final PatientProcedureRepository patientProcedureRepository;
  private final PatientRepository patientRepository;
  private final DoctorProcedureMapper assignmentMapper;
  private final ProcedureMapper procedureMapper;
  private final FieldTypePresetRepository presetRepository;

  public DoctorProcedureConfigurationServiceImpl(UserRepository userRepository,
      DoctorRepository doctorRepository,
      DoctorProcedureRepository doctorProcedureRepository,
      DoctorProcedureFieldRepository fieldRepository,
      PatientProcedureRepository patientProcedureRepository,
      PatientRepository patientRepository,
      DoctorProcedureMapper assignmentMapper,
      ProcedureMapper procedureMapper,
      FieldTypePresetRepository presetRepository) {
    this.userRepository = userRepository;
    this.doctorRepository = doctorRepository;
    this.doctorProcedureRepository = doctorProcedureRepository;
    this.fieldRepository = fieldRepository;
    this.patientProcedureRepository = patientProcedureRepository;
    this.patientRepository = patientRepository;
    this.assignmentMapper = assignmentMapper;
    this.procedureMapper = procedureMapper;
    this.presetRepository = presetRepository;
  }

  @Override
  @Transactional(readOnly = true)
  public List<FieldTypePresetResponse> listFieldTypePresets() {
    return presetRepository.findByActiveTrueOrderByNameAsc().stream()
        .map(preset -> new FieldTypePresetResponse(preset.getId(), preset.getName(), preset.getDescription(),
            preset.getDataType(), preset.getMinValue(), preset.getMaxValue(), preset.getInputStyle()))
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DoctorProcedureResponse> listOwnProcedures(String email) {
    Doctor doctor = resolveDoctor(email);
    return doctorProcedureRepository.findByDoctorId(doctor.getId()).stream()
        .filter(dp -> Boolean.TRUE.equals(dp.getProcedure().getActive()))
        .map(assignmentMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<DoctorProcedureFieldResponse> listFields(String email, UUID doctorProcedureId) {
    DoctorProcedure assignment = findOwnAssignment(email, doctorProcedureId);
    return fieldRepository.findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(assignment.getId()).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public List<DoctorProcedureFieldResponse> replaceFields(String email, UUID doctorProcedureId,
      List<DoctorProcedureFieldRequest> requests) {
    DoctorProcedure assignment = findOwnAssignment(email, doctorProcedureId);
    List<DoctorProcedureField> current = fieldRepository
        .findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(assignment.getId());
    current.forEach(field -> field.setActive(false));
    fieldRepository.saveAll(current);

    List<DoctorProcedureField> fields = requests == null ? List.of()
        : requests.stream()
            .map(request -> toEntity(request, assignment))
            .toList();
    return fieldRepository.saveAll(fields).stream().map(this::toResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CheckinFormResponse getPatientCheckinForm(String email, UUID patientProcedureId) {
    Patient patient = patientRepository.findByUserId(resolveUserId(email))
        .orElseThrow(() -> new UnauthorizedException("Paciente não encontrado para o usuário autenticado"));

    PatientProcedure assignment = patientProcedureRepository.findById(patientProcedureId)
        .orElseThrow(() -> new ResourceNotFoundException("Acompanhamento não encontrado"));

    if (!assignment.getPatient().getId().equals(patient.getId())) {
      throw new UnauthorizedException("Este acompanhamento não pertence ao paciente autenticado");
    }

    DoctorProcedure doctorProcedure = doctorProcedureRepository
        .findByDoctorIdAndProcedureId(assignment.getDoctor().getId(), assignment.getProcedure().getId())
        .orElseThrow(() -> new ResourceNotFoundException("Configuração do procedimento não encontrada"));

    List<CheckinFormResponse.Field> fields = fieldRepository
        .findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(doctorProcedure.getId()).stream()
        .map(field -> new CheckinFormResponse.Field(field.getId(), field.getName(), field.getDescription(),
            field.getUnit(), field.getDataType(), field.getMetricKey(), field.getRequired(),
            field.getDisplayOrder(), field.getMinValue(), field.getMaxValue(), field.getNormalBoolean()))
        .toList();

    return new CheckinFormResponse(patientProcedureId, procedureMapper.toSummary(assignment.getProcedure()), fields);
  }

  private DoctorProcedureField toEntity(DoctorProcedureFieldRequest request, DoctorProcedure assignment) {
    DoctorProcedureField field = new DoctorProcedureField();

    field.setDoctorProcedure(assignment);
    field.setName(request.name().trim());
    field.setDescription(request.description());
    field.setUnit(request.unit());
    field.setDataType(request.dataType());
    field.setMetricKey(request.metricKey());
    field.setRequired(Boolean.TRUE.equals(request.required()));
    field.setDisplayOrder(request.displayOrder() == null ? 0 : request.displayOrder());
    field.setMinValue(request.minValue());
    field.setMaxValue(request.maxValue());
    field.setNormalBoolean(request.normalBoolean());

    if (request.thresholds() != null) {
      request.thresholds().forEach(item -> {
        FieldThreshold threshold = new FieldThreshold();

        threshold.setField(field);
        threshold.setSeverityOrder(item.severityOrder());
        threshold.setLabel(item.label().trim());
        threshold.setColor(item.color());
        threshold.setMinValue(item.minValue());
        threshold.setMaxValue(item.maxValue());

        field.getThresholds().add(threshold);
      });
    }

    return field;
  }

  private DoctorProcedureFieldResponse toResponse(DoctorProcedureField field) {
    List<DoctorProcedureFieldResponse.FieldThresholdResponse> thresholds = field.getThresholds().stream()
        .sorted(Comparator.comparing(FieldThreshold::getSeverityOrder).reversed())
        .map(item -> new DoctorProcedureFieldResponse.FieldThresholdResponse(item.getId(), item.getSeverityOrder(),
            item.getLabel(), item.getColor(), item.getMinValue(), item.getMaxValue()))
        .toList();

    return new DoctorProcedureFieldResponse(field.getId(), field.getName(), field.getDescription(), field.getUnit(),
        field.getDataType(), field.getMetricKey(), field.getRequired(), field.getDisplayOrder(),
        field.getMinValue(), field.getMaxValue(), field.getNormalBoolean(), field.getActive(), thresholds);
  }

  private DoctorProcedure findOwnAssignment(String email, UUID id) {
    Doctor doctor = resolveDoctor(email);

    DoctorProcedure assignment = doctorProcedureRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Procedimento do médico não encontrado"));

    if (!assignment.getDoctor().getId().equals(doctor.getId())) {
      throw new UnauthorizedException("O procedimento não pertence ao médico autenticado");
    }

    return assignment;
  }

  private Doctor resolveDoctor(String email) {
    UUID userId = resolveUserId(email);

    return doctorRepository.findByUserId(userId)
        .filter(doctor -> Boolean.TRUE.equals(doctor.getActive()))
        .orElseThrow(() -> new UnauthorizedException("Perfil do médico não encontrado"));
  }

  private UUID resolveUserId(String email) {
    return userRepository.findByEmailAndActiveTrue(email)
        .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"))
        .getId();
  }
}
