package com.tcc.application.service;

import com.tcc.application.dto.request.ManualCheckinRequest;
import com.tcc.application.dto.response.CheckinResponse;
import com.tcc.domain.model.Checkin;
import com.tcc.domain.model.CheckinFieldValue;
import com.tcc.domain.model.CheckinSource;
import com.tcc.domain.model.DoctorProcedure;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldDataType;
import com.tcc.domain.model.Patient;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ErrorMessages;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CheckinServiceImpl implements CheckinService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PatientProcedureRepository patientProcedureRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final DoctorProcedureFieldRepository fieldRepository;
    private final CheckinRepository checkinRepository;

    public CheckinServiceImpl(UserRepository userRepository,
                              PatientRepository patientRepository,
                              PatientProcedureRepository patientProcedureRepository,
                              DoctorProcedureRepository doctorProcedureRepository,
                              DoctorProcedureFieldRepository fieldRepository,
                              CheckinRepository checkinRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.patientProcedureRepository = patientProcedureRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.fieldRepository = fieldRepository;
        this.checkinRepository = checkinRepository;
    }

    @Override
    @Transactional
    public CheckinResponse submitManual(String email, UUID patientProcedureId, ManualCheckinRequest request) {
        Patient patient = patientRepository.findByUserId(resolveUserId(email))
                .orElseThrow(() -> new UnauthorizedException("Paciente não encontrado para o usuário autenticado"));
        PatientProcedure patientProcedure = patientProcedureRepository.findById(patientProcedureId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorMessages.patientProcedureNotFoundById(patientProcedureId)));

        if (!patientProcedure.getPatient().getId().equals(patient.getId())) {
            throw new UnauthorizedException("Este acompanhamento não pertence ao paciente autenticado");
        }

        LocalDate today = LocalDate.now();
        validateActive(patientProcedure, today);
        if (checkinRepository.existsByPatientProcedureIdAndSourceAndManualDate(
                patientProcedureId, CheckinSource.MANUAL, today)) {
            throw new BusinessException("Já existe um check-in manual para este acompanhamento hoje");
        }

        DoctorProcedure doctorProcedure = doctorProcedureRepository
                .findByDoctorIdAndProcedureId(patientProcedure.getDoctor().getId(), patientProcedure.getProcedure().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Configuração do procedimento não encontrada"));
        List<DoctorProcedureField> activeFields =
                fieldRepository.findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(doctorProcedure.getId());
        Map<UUID, DoctorProcedureField> fieldsById = new HashMap<>();
        activeFields.forEach(field -> fieldsById.put(field.getId(), field));

        Set<UUID> submittedFieldIds = new HashSet<>();
        for (ManualCheckinRequest.FieldValue submittedValue : request.fields()) {
            DoctorProcedureField field = fieldsById.get(submittedValue.fieldId());
            if (field == null) {
                throw new BusinessException("Campo informado não está ativo neste acompanhamento: "
                        + submittedValue.fieldId());
            }
            if (!submittedFieldIds.add(field.getId())) {
                throw new BusinessException("Um campo não pode ser informado mais de uma vez: " + field.getName());
            }
            validateValue(field, submittedValue);
        }

        activeFields.stream()
                .filter(field -> Boolean.TRUE.equals(field.getRequired()))
                .filter(field -> !submittedFieldIds.contains(field.getId()))
                .findFirst()
                .ifPresent(field -> {
                    throw new BusinessException("O campo obrigatório não foi informado: " + field.getName());
                });

        LocalDateTime submittedAt = LocalDateTime.now();
        Checkin checkin = new Checkin();
        checkin.setPatientProcedure(patientProcedure);
        checkin.setSource(CheckinSource.MANUAL);
        checkin.setManualDate(today);
        checkin.setSubmittedAt(submittedAt);

        for (ManualCheckinRequest.FieldValue submittedValue : request.fields()) {
            CheckinFieldValue fieldValue = new CheckinFieldValue();
            fieldValue.setCheckin(checkin);
            fieldValue.setField(fieldsById.get(submittedValue.fieldId()));
            fieldValue.setRawValue(normalize(submittedValue.value()));
            fieldValue.setPhotoUrl(normalize(submittedValue.photoUrl()));
            checkin.getFieldValues().add(fieldValue);
        }

        Checkin saved = checkinRepository.save(checkin);
        return new CheckinResponse(saved.getId(), patientProcedureId, saved.getSource(), saved.getSubmittedAt());
    }

    private UUID resolveUserId(String email) {
        return userRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.userNotFoundByEmail(email)))
                .getId();
    }

    private void validateActive(PatientProcedure patientProcedure, LocalDate today) {
        if (!"EM_ANDAMENTO".equals(patientProcedure.getStatus())
                || today.isBefore(patientProcedure.getStartDate())
                || (patientProcedure.getEndDate() != null && today.isAfter(patientProcedure.getEndDate()))) {
            throw new BusinessException("O acompanhamento não está em andamento para receber check-in");
        }
    }

    private void validateValue(DoctorProcedureField field, ManualCheckinRequest.FieldValue submittedValue) {
        String value = normalize(submittedValue.value());
        String photoUrl = normalize(submittedValue.photoUrl());

        if (field.getDataType() == FieldDataType.PHOTO) {
            if (photoUrl == null) {
                throw new BusinessException("O campo de foto exige uma URL: " + field.getName());
            }
            if (value != null) {
                throw new BusinessException("O campo de foto não aceita valor textual: " + field.getName());
            }
            return;
        }
        if (value == null) {
            throw new BusinessException("O campo exige um valor: " + field.getName());
        }
        if (photoUrl != null) {
            throw new BusinessException("URL de foto só é aceita para campos de foto: " + field.getName());
        }

        switch (field.getDataType()) {
            case INTEGER -> validateInteger(value, field);
            case DECIMAL, SCALE -> validateNumber(value, field);
            case BOOLEAN -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessException("O campo booleano deve ser true ou false: " + field.getName());
                }
            }
            case TEXT -> {
                // A presença de texto não vazio já é garantida pela normalização.
            }
            case PHOTO -> throw new IllegalStateException("Campo de foto já foi validado");
        }
    }

    private void validateInteger(String value, DoctorProcedureField field) {
        if (!value.matches("[+-]?\\d+")) {
            throw new BusinessException("O campo deve receber um número inteiro: " + field.getName());
        }
        validateNumericRange(new BigDecimal(value), field);
    }

    private void validateNumber(String value, DoctorProcedureField field) {
        final BigDecimal number;
        try {
            number = new BigDecimal(value);
        } catch (NumberFormatException ex) {
            throw new BusinessException("O campo deve receber um número válido: " + field.getName());
        }
        validateNumericRange(number, field);
    }

    private void validateNumericRange(BigDecimal number, DoctorProcedureField field) {
        if (field.getMinValue() != null && number.compareTo(BigDecimal.valueOf(field.getMinValue())) < 0) {
            throw new BusinessException("O valor está abaixo do mínimo permitido para: " + field.getName());
        }
        if (field.getMaxValue() != null && number.compareTo(BigDecimal.valueOf(field.getMaxValue())) > 0) {
            throw new BusinessException("O valor está acima do máximo permitido para: " + field.getName());
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
