package com.tcc.application.service;

import com.tcc.application.dto.response.DailyAverageResponse;
import com.tcc.application.dto.response.DailyCheckinStatusResponse;
import com.tcc.application.dto.response.PatientFieldTrendResponse;
import com.tcc.application.dto.response.PatientHomeMetricsResponse;
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
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientHomeMetricsServiceImpl implements PatientHomeMetricsService {
    private static final List<FieldDataType> NUMERIC_TYPES = List.of(
            FieldDataType.INTEGER, FieldDataType.DECIMAL, FieldDataType.SCALE);
    private static final int WEEK_LENGTH_DAYS = 7;

    private final PatientRepository patientRepository;
    private final PatientProcedureRepository patientProcedureRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final DoctorProcedureFieldRepository fieldRepository;
    private final CheckinRepository checkinRepository;

    public PatientHomeMetricsServiceImpl(
            PatientRepository patientRepository,
            PatientProcedureRepository patientProcedureRepository,
            DoctorProcedureRepository doctorProcedureRepository,
            DoctorProcedureFieldRepository fieldRepository,
            CheckinRepository checkinRepository) {
        this.patientRepository = patientRepository;
        this.patientProcedureRepository = patientProcedureRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.fieldRepository = fieldRepository;
        this.checkinRepository = checkinRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientHomeMetricsResponse getHomeMetrics(
            String email, UUID patientProcedureId, LocalDate startDate, LocalDate endDate) {
        validateWeek(startDate, endDate);
        Patient patient = patientRepository.findByEmailAndActiveTrue(email)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado"));
        PatientProcedure patientProcedure = patientProcedureRepository.findByIdAndActiveTrue(patientProcedureId)
                .filter(assignment -> Objects.equals(assignment.getPatient().getId(), patient.getId()))
                .orElseThrow(() -> new ResourceNotFoundException("Acompanhamento não encontrado"));

        LocalDate today = LocalDate.now();
        Set<LocalDate> submittedDays = checkinRepository.findDailyManualCheckinDates(
                patient.getId(), startDate, endDate, CheckinSource.MANUAL).stream().collect(Collectors.toSet());
        List<DailyCheckinStatusResponse> dailyCheckins = new ArrayList<>(WEEK_LENGTH_DAYS);
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            dailyCheckins.add(new DailyCheckinStatusResponse(day, submittedDays.contains(day)));
        }

        DoctorProcedure doctorProcedure = doctorProcedureRepository
                .findByDoctorIdAndProcedureIdAndActiveTrue(
                        patientProcedure.getDoctor().getId(), patientProcedure.getProcedure().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Configuração do procedimento não encontrada"));
        List<DoctorProcedureField> numericFields = fieldRepository
                .findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(doctorProcedure.getId())
                .stream()
                .filter(field -> NUMERIC_TYPES.contains(field.getDataType()))
                .toList();
        Map<UUID, Map<LocalDate, Aggregate>> fieldAverages = new HashMap<>();
        for (DoctorProcedureField field : numericFields) {
            Map<LocalDate, Aggregate> days = new HashMap<>();
            for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
                days.put(day, new Aggregate());
            }
            fieldAverages.put(field.getId(), days);
        }

        if (!numericFields.isEmpty()) {
            List<UUID> fieldIds = numericFields.stream().map(DoctorProcedureField::getId).toList();
            for (Object[] row : checkinRepository.findManualNumericFieldValues(
                    patient.getId(), patientProcedureId, fieldIds, NUMERIC_TYPES, startDate, endDate,
                    CheckinSource.MANUAL)) {
                UUID fieldId = (UUID) row[0];
                LocalDate date = (LocalDate) row[1];
                String rawValue = (String) row[2];
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }
                Map<LocalDate, Aggregate> fieldDays = fieldAverages.get(fieldId);
                Aggregate aggregate = fieldDays == null ? null : fieldDays.get(date);
                if (aggregate != null) {
                    aggregate.add(new BigDecimal(rawValue.trim()));
                }
            }
        }

        List<PatientFieldTrendResponse> trends = numericFields.stream()
                .map(field -> new PatientFieldTrendResponse(
                        field.getId(),
                        field.getName(),
                        field.getDataType(),
                        field.getUnit(),
                        fieldAverages.get(field.getId()).entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> new DailyAverageResponse(
                                        entry.getKey(), entry.getValue().average(), entry.getValue().count()))
                                .toList()))
                .toList();

        return new PatientHomeMetricsResponse(
                patientProcedureId,
                patientProcedure.getProcedure().getTitle(),
                patientProcedure.getStartDate(),
                checkinRepository.existsManualCheckinByPatientIdAndManualDate(
                        patient.getId(), CheckinSource.MANUAL, today),
                dailyCheckins,
                trends);
    }

    private void validateWeek(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null
                || endDate.toEpochDay() - startDate.toEpochDay() != WEEK_LENGTH_DAYS - 1) {
            throw new BusinessException("O período deve conter exatamente 7 dias inclusivos");
        }
    }

    private static final class Aggregate {
        private BigDecimal total = BigDecimal.ZERO;
        private long count;

        void add(BigDecimal value) {
            total = total.add(value);
            count++;
        }

        BigDecimal average() {
            return count == 0 ? null : total.divide(BigDecimal.valueOf(count), MathContext.DECIMAL128);
        }

        long count() {
            return count;
        }
    }
}
