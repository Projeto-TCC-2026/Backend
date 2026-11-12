package com.tcc.application.service;

import com.tcc.application.dto.response.DailyAverageResponse;
import com.tcc.application.dto.response.DailyMetricResponse;
import com.tcc.application.dto.response.ProcedureFieldMetricsResponse;
import com.tcc.application.dto.response.ProcedureMetricsResponse;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.DoctorProcedureField;
import com.tcc.domain.model.FieldDataType;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.CheckinSubmissionRepository;
import com.tcc.domain.repository.DoctorProcedureFieldRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.HealthReadingRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.BusinessException;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import java.math.BigDecimal;
import java.math.MathContext;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PresentationMetricsServiceImpl implements PresentationMetricsService {

    private static final List<FieldDataType> NUMERIC_TYPES = List.of(FieldDataType.INTEGER, FieldDataType.DECIMAL,
            FieldDataType.SCALE);
    private static final int AVERAGE_LOOKBACK_DAYS = 7;

    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final CheckinSubmissionRepository checkinSubmissionRepository;
    private final HealthReadingRepository healthReadingRepository;
    private final AlertRepository alertRepository;
    private final ProcedureRepository procedureRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final DoctorProcedureFieldRepository fieldRepository;
    private final CheckinRepository checkinRepository;

    public PresentationMetricsServiceImpl(UserRepository userRepository,
            DoctorRepository doctorRepository,
            CheckinSubmissionRepository checkinSubmissionRepository,
            HealthReadingRepository healthReadingRepository,
            AlertRepository alertRepository,
            ProcedureRepository procedureRepository,
            DoctorProcedureRepository doctorProcedureRepository,
            DoctorProcedureFieldRepository fieldRepository,
            CheckinRepository checkinRepository) {
        this.userRepository = userRepository;
        this.doctorRepository = doctorRepository;
        this.checkinSubmissionRepository = checkinSubmissionRepository;
        this.healthReadingRepository = healthReadingRepository;
        this.alertRepository = alertRepository;
        this.procedureRepository = procedureRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.fieldRepository = fieldRepository;
        this.checkinRepository = checkinRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMetricResponse> getDailyCheckinSubmissions(String email,
            LocalDate startDate,
            LocalDate endDate) {
        Scope scope = resolveScope(email);
        validateDateRange(startDate, endDate);
        return dailyCounts(startDate, endDate, checkinSubmissionRepository.countDailySubmissions(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(),
                scope.doctorId(), scope.hospitalId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMetricResponse> getDailyPatientsWithReadings(String email,
            LocalDate startDate,
            LocalDate endDate) {
        Scope scope = resolveScope(email);
        validateDateRange(startDate, endDate);
        return dailyCounts(startDate, endDate, healthReadingRepository.countDailyPatientsWithReadings(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(),
                scope.doctorId(), scope.hospitalId()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DailyMetricResponse> getDailyAlerts(String email,
            LocalDate startDate,
            LocalDate endDate) {
        Scope scope = resolveScope(email);
        validateDateRange(startDate, endDate);
        return dailyCounts(startDate, endDate, alertRepository.countDailyReportableAlerts(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(),
                scope.doctorId(), scope.hospitalId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureMetricsResponse getProcedureMetrics(String email, UUID procedureId,
            LocalDate startDate, LocalDate endDate) {
        Scope scope = resolveScope(email);
        validateDateRange(startDate, endDate);
        if (endDate.toEpochDay() - startDate.toEpochDay() != 6) {
            throw new BusinessException("O período do procedimento deve conter exatamente 7 dias inclusivos");
        }

        if (!procedureRepository.existsById(procedureId)) {
            throw new ResourceNotFoundException("Procedimento não encontrado");
        }
        boolean accessible = switch (scope.role()) {
            case ADMIN -> true;
            case DOCTOR -> doctorProcedureRepository.existsAccessibleProcedure(
                    procedureId, scope.doctorId(), null);
            case HOSPITAL -> procedureRepository.existsByIdAndHospitalId(
                    procedureId, scope.hospitalId());
            case PATIENT -> false;
        };
        if (!accessible) {
            throw new UnauthorizedException("Você não tem permissão para acessar as métricas deste procedimento");
        }

        LocalDate dataStartDate = startDate.minusDays(AVERAGE_LOOKBACK_DAYS);
        LocalDateTime dataStart = dataStartDate.atStartOfDay();
        LocalDateTime dataEnd = endDate.plusDays(1).atStartOfDay();
        List<DoctorProcedureField> fields = fieldRepository.findMetricsFields(
                procedureId, scope.doctorId(), scope.hospitalId());
        List<DoctorProcedureField> numericFields = fields.stream()
                .filter(field -> NUMERIC_TYPES.contains(field.getDataType()))
                .toList();

        Map<MetricKey, FieldAggregate> aggregates = new LinkedHashMap<>();
        Map<UUID, MetricKey> fieldMetricKeys = new HashMap<>();
        for (DoctorProcedureField field : numericFields) {
            MetricKey key = metricKey(field);
            fieldMetricKeys.put(field.getId(), key);
            FieldAggregate aggregate = aggregates.computeIfAbsent(
                    key, ignored -> new FieldAggregate(field, new ArrayList<>(), dailyAggregates(dataStartDate, endDate)));
            aggregate.sourceFieldIds().add(field.getId());
        }

        if (!numericFields.isEmpty()) {
            List<UUID> fieldIds = numericFields.stream().map(DoctorProcedureField::getId).toList();
            List<Object[]> values = checkinRepository.findNumericFieldValues(
                    fieldIds, NUMERIC_TYPES, dataStart, dataEnd, scope.doctorId(), scope.hospitalId());
            for (Object[] row : values) {
                UUID fieldId = (UUID) row[0];
                LocalDate date = toLocalDate(row[1]);
                String rawValue = (String) row[2];
                if (rawValue == null || rawValue.isBlank()) {
                    continue;
                }
                MetricKey key = fieldMetricKeys.get(fieldId);
                FieldAggregate fieldAggregate = key == null ? null : aggregates.get(key);
                Aggregate aggregate = fieldAggregate == null ? null : fieldAggregate.dailyAverages().get(date);
                if (aggregate != null) {
                    aggregate.add(new BigDecimal(rawValue.trim()));
                }
            }
        }

        List<ProcedureFieldMetricsResponse> responseFields = aggregates.values().stream()
                .map(fieldAggregate -> new ProcedureFieldMetricsResponse(
                        fieldAggregate.representative().getId(),
                        List.copyOf(fieldAggregate.sourceFieldIds()),
                        fieldAggregate.representative().getName(),
                        fieldAggregate.representative().getDataType(),
                        fieldAggregate.representative().getUnit(),
                        fieldAggregate.dailyAverages().entrySet().stream()
                                .sorted(Map.Entry.comparingByKey())
                                .map(entry -> new DailyAverageResponse(
                                        entry.getKey(),
                                        entry.getValue().average(),
                                        entry.getValue().count()))
                                .toList()))
                .toList();

        return new ProcedureMetricsResponse(
                procedureId, startDate, endDate, dataStartDate, endDate, responseFields);
    }

    private MetricKey metricKey(DoctorProcedureField field) {
        String normalizedName = field.getName().trim().toLowerCase(Locale.ROOT);
        String normalizedUnit = field.getUnit() == null ? "" : field.getUnit().trim();
        return new MetricKey(normalizedName, field.getDataType(), normalizedUnit);
    }

    private Map<LocalDate, Aggregate> dailyAggregates(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, Aggregate> dailyAggregates = new HashMap<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            dailyAggregates.put(day, new Aggregate());
        }
        return dailyAggregates;
    }

    private Scope resolveScope(String email) {
        User user = userRepository.findByEmailWithHospital(email)
                .orElseThrow(() -> new UnauthorizedException("Usuário autenticado não encontrado"));

        return switch (user.getRole()) {
            case ADMIN -> new Scope(Role.ADMIN, null, null);
            case DOCTOR -> {
                Doctor doctor = doctorRepository.findByUserId(user.getId())
                        .orElseThrow(() -> new UnauthorizedException("Perfil de médico não encontrado"));
                yield new Scope(Role.DOCTOR, doctor.getId(), null);
            }
            case HOSPITAL -> {
                if (user.getHospital() == null) {
                    throw new UnauthorizedException("Perfil de hospital não encontrado");
                }
                yield new Scope(Role.HOSPITAL, null, user.getHospital().getId());
            }
            case PATIENT -> throw new UnauthorizedException("Pacientes não têm acesso às métricas administrativas");
        };
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new BusinessException("As datas de início e fim são obrigatórias");
        }
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim");
        }
    }

    private List<DailyMetricResponse> dailyCounts(LocalDate startDate, LocalDate endDate,
            List<Object[]> groupedCounts) {
        Map<LocalDate, Long> counts = groupedCounts.stream().collect(Collectors.toMap(
                row -> LocalDate.of(((Number) row[0]).intValue(),
                        ((Number) row[1]).intValue(), ((Number) row[2]).intValue()),
                row -> ((Number) row[3]).longValue()));
        List<DailyMetricResponse> result = new ArrayList<>();
        for (LocalDate day = startDate; !day.isAfter(endDate); day = day.plusDays(1)) {
            result.add(new DailyMetricResponse(day, counts.getOrDefault(day, 0L)));
        }
        return result;
    }

    private LocalDate toLocalDate(Object value) {
        if (value instanceof LocalDate date) {
            return date;
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toLocalDate();
        }
        if (value instanceof java.sql.Date date) {
            return date.toLocalDate();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toLocalDateTime().toLocalDate();
        }
        if (value instanceof Date date) {
            return new Timestamp(date.getTime()).toLocalDateTime().toLocalDate();
        }
        if (value instanceof String date) {
            return LocalDate.parse(date);
        }
        throw new IllegalStateException("Tipo de data inesperado retornado pela consulta: "
                + (value == null ? "null" : value.getClass().getName()));
    }

    private record Scope(Role role, UUID doctorId, UUID hospitalId) {
    }

    private record MetricKey(String name, FieldDataType dataType, String unit) {
    }

    private record FieldAggregate(
            DoctorProcedureField representative,
            List<UUID> sourceFieldIds,
            Map<LocalDate, Aggregate> dailyAverages) {
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
