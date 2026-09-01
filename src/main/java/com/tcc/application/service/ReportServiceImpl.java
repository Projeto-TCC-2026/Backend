package com.tcc.application.service;

import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.domain.model.Alert;
import com.tcc.domain.model.Checkin;
import com.tcc.domain.model.CheckinFieldValue;
import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.ResourceNotFoundException;
import com.tcc.exception.UnauthorizedException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@Service
public class ReportServiceImpl implements ReportService {

    private final PatientRepository patientRepository;
    private final DoctorRepository doctorRepository;
    private final ProcedureRepository procedureRepository;
    private final UserRepository userRepository;
    private final DoctorProcedureRepository doctorProcedureRepository;
    private final PatientProcedureRepository patientProcedureRepository;
    private final CheckinRepository checkinRepository;
    private final AlertRepository alertRepository;

    public ReportServiceImpl(PatientRepository patientRepository,
            DoctorRepository doctorRepository,
            ProcedureRepository procedureRepository,
            UserRepository userRepository,
            DoctorProcedureRepository doctorProcedureRepository,
            PatientProcedureRepository patientProcedureRepository,
            CheckinRepository checkinRepository,
            AlertRepository alertRepository) {
        this.patientRepository = patientRepository;
        this.doctorRepository = doctorRepository;
        this.procedureRepository = procedureRepository;
        this.userRepository = userRepository;
        this.doctorProcedureRepository = doctorProcedureRepository;
        this.patientProcedureRepository = patientProcedureRepository;
        this.checkinRepository = checkinRepository;
        this.alertRepository = alertRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientsByHospitalResponse> getPatientsByHospital() {
        List<Object[]> results = patientRepository.countPatientsByHospital();

        return results.stream()
                .map(row -> new PatientsByHospitalResponse(
                        (UUID) row[0], // hospitalId
                        (String) row[1], // hospitalName
                        ((Number) row[2]).longValue() // totalPatients
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoctorsByHospitalResponse> getDoctorsByHospital() {
        List<Object[]> results = doctorRepository.countDoctorsByHospital();

        return results.stream()
                .map(row -> new DoctorsByHospitalResponse(
                        (UUID) row[0], // hospitalId
                        (String) row[1], // hospitalName
                        ((Number) row[2]).longValue() // totalDoctors
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByDoctorResponse> getProceduresByDoctor() {
        List<Object[]> results = procedureRepository.countProceduresByDoctor();

        return results.stream()
                .map(row -> new ProceduresByDoctorResponse(
                        (UUID) row[0], // doctorId
                        (String) row[1], // doctorName
                        (String) row[2], // specialty
                        ((Number) row[3]).longValue() // totalProcedures
                ))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProceduresByPeriodResponse> getProceduresByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("As datas de início e fim são obrigatórias");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("A data de início não pode ser posterior à data de fim");
        }

        List<Object[]> results = procedureRepository.countProceduresByPeriod(startDate, endDate);

        return results.stream()
                .map(row -> new ProceduresByPeriodResponse(
                        formatPeriod((Number) row[0], (Number) row[1]),
                        ((Number) row[2]).longValue()))
                .collect(Collectors.toList());
    }

    private String formatPeriod(Number year, Number month) {
        return "%04d-%02d".formatted(year.intValue(), month.intValue());
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportCheckins(String email, LocalDate startDate, LocalDate endDate, UUID procedureId,
            UUID patientId, UUID doctorId) {
        ExportFilters filters = validateFilters(email, procedureId, patientId, doctorId);
        List<Checkin> checkins = checkinRepository.findForReport(
                startDate.atStartOfDay(), endDate.plusDays(1).atStartOfDay(), filters.procedureId(),
                filters.patientId(), filters.doctorId(), filters.hospitalId());

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Map<UUID, List<Checkin>> byProcedure = checkins.stream()
                    .collect(Collectors.groupingBy(
                            checkin -> checkin.getPatientProcedure().getProcedure().getId(),
                            LinkedHashMap::new,
                            Collectors.toList()));

            if (byProcedure.isEmpty()) {
                writeCheckinSheet(workbook.createSheet("Check-ins"), List.of());
            } else {
                for (List<Checkin> procedureCheckins : byProcedure.values()) {
                    String procedureName = procedureCheckins.get(0).getPatientProcedure().getProcedure().getTitle();
                    writeCheckinSheet(workbook.createSheet(WorkbookUtil.createSafeSheetName(procedureName)),
                            procedureCheckins);
                }
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o arquivo XLSX", ex);
        }
    }

    private void writeCheckinSheet(Sheet sheet, List<Checkin> checkins) {
        writeHeader(sheet, "Data/hora de envio", "Paciente", "Médico", "Procedimento",
                "Campo", "Valor", "Unidade", "Origem");
        int rowIndex = 1;
        for (Checkin checkin : checkins) {
            for (CheckinFieldValue fieldValue : checkin.getFieldValues()) {
                Row row = sheet.createRow(rowIndex++);
                PatientProcedure assignment = checkin.getPatientProcedure();
                set(row, 0, checkin.getSubmittedAt().toString());
                set(row, 1, assignment.getPatient().getFullName());
                set(row, 2, assignment.getDoctor().getFullName());
                set(row, 3, assignment.getProcedure().getTitle());
                set(row, 4, fieldValue.getField().getName());
                set(row, 5, fieldValue.getRawValue() == null ? fieldValue.getPhotoUrl() : fieldValue.getRawValue());
                set(row, 6, fieldValue.getField().getUnit());
                set(row, 7, checkin.getSource().name());
            }
        }
        setWidths(sheet, 8);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportAlerts(String email, LocalDate startDate, LocalDate endDate, UUID procedureId,
            UUID patientId, UUID doctorId) {
        ExportFilters filters = validateFilters(email, procedureId, patientId, doctorId);
        LocalDateTime start = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime end = endDate == null ? null : endDate.plusDays(1).atStartOfDay();
        List<Alert> alerts = alertRepository.findForReport(start, end);

        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Alertas");
            writeHeader(sheet, "Data/hora de emissão", "Paciente", "Médico", "Procedimento",
                    "Dado clínico/Métrica", "Valor recebido", "Origem", "Severidade", "Rótulo", "Status");
            int rowIndex = 1;
            for (Alert alert : alerts) {
                if (filters.patientId() != null && !filters.patientId().equals(alert.getPatient().getId())) {
                    continue;
                }
                Optional<PatientProcedure> context = findAlertContext(alert, filters);
                if (context.isEmpty()) {
                    continue;
                }
                PatientProcedure assignment = context.get();
                Row row = sheet.createRow(rowIndex++);
                set(row, 0, alert.getCreatedAt().toString());
                set(row, 1, alert.getPatient().getFullName());
                set(row, 2, assignment.getDoctor().getFullName());
                set(row, 3, assignment.getProcedure().getTitle());
                set(row, 4, alert.getHealthReading() == null ? alert.getTitle()
                        : alert.getHealthReading().getReadingType());
                set(row, 5, alert.getHealthReading() == null ? alert.getDescription()
                        : alert.getHealthReading().getValue());
                set(row, 6, alert.getHealthReading() == null ? "N/A" : "SMARTWATCH");
                set(row, 7, alert.getSeverity());
                set(row, 8, alert.getTitle());
                set(row, 9, alert.getStatus());
            }
            setWidths(sheet, 10);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o arquivo XLSX", ex);
        }
    }

    private ExportFilters validateFilters(String email, UUID procedureId, UUID patientId, UUID requestedDoctorId) {
        ReportScope scope = resolveScope(email);
        UUID doctorId = scope.doctorId();

        if (scope.doctorId() != null && requestedDoctorId != null && !scope.doctorId().equals(requestedDoctorId)) {
            throw new UnauthorizedException("O médico informado não corresponde ao usuário autenticado");
        }
        if (scope.hospitalId() != null && requestedDoctorId != null) {
            Doctor requestedDoctor = doctorRepository.findById(requestedDoctorId)
                    .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado"));
            if (!requestedDoctor.getHospital().getId().equals(scope.hospitalId())) {
                throw new UnauthorizedException("O médico informado não pertence ao seu hospital");
            }
            doctorId = requestedDoctorId;
        }

        if (procedureId != null) {
            Procedure procedure = procedureRepository.findById(procedureId)
                    .orElseThrow(() -> new ResourceNotFoundException("Procedimento não encontrado"));
            if (scope.doctorId() != null
                    && !doctorProcedureRepository.existsByDoctorIdAndProcedureId(scope.doctorId(), procedureId)) {
                throw new UnauthorizedException("O procedimento não pertence ao médico autenticado");
            }
            if (scope.hospitalId() != null && !procedure.getHospital().getId().equals(scope.hospitalId())) {
                throw new UnauthorizedException("O procedimento não pertence ao seu hospital");
            }
        }

        return new ExportFilters(procedureId, patientId, doctorId, scope.hospitalId());
    }

    private ReportScope resolveScope(String email) {
        User user = userRepository.findByEmailWithHospital(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário autenticado não encontrado"));
        if (user.getRole() == Role.DOCTOR) {
            Doctor doctor = doctorRepository.findByUserId(user.getId())
                    .filter(item -> Boolean.TRUE.equals(item.getActive()))
                    .orElseThrow(() -> new UnauthorizedException("Perfil do médico não encontrado"));
            return new ReportScope(doctor.getId(), null);
        }
        if (user.getRole() == Role.HOSPITAL && user.getHospital() != null
                && Boolean.TRUE.equals(user.getHospital().getActive())) {
            return new ReportScope(null, user.getHospital().getId());
        }
        throw new UnauthorizedException("Seu perfil não pode emitir relatórios");
    }

    private Optional<PatientProcedure> findAlertContext(Alert alert, ExportFilters filters) {
        return patientProcedureRepository.findByPatientId(alert.getPatient().getId()).stream()
                .filter(assignment -> filters.doctorId() == null
                        || filters.doctorId().equals(assignment.getDoctor().getId()))
                .filter(assignment -> filters.hospitalId() == null
                        || filters.hospitalId().equals(assignment.getDoctor().getHospital().getId()))
                .filter(assignment -> filters.procedureId() == null
                        || filters.procedureId().equals(assignment.getProcedure().getId()))
                .filter(assignment -> !alert.getCreatedAt().toLocalDate().isBefore(assignment.getStartDate()))
                .filter(assignment -> assignment.getEndDate() == null
                        || !alert.getCreatedAt().toLocalDate().isAfter(assignment.getEndDate()))
                .max(Comparator.comparing(PatientProcedure::getStartDate));
    }

    private void writeHeader(Sheet sheet, String... labels) {
        Row row = sheet.createRow(0);
        for (int index = 0; index < labels.length; index++) {
            set(row, index, labels[index]);
        }
    }

    private void set(Row row, int column, String value) {
        row.createCell(column).setCellValue(value == null ? "" : value);
    }

    private void setWidths(Sheet sheet, int columns) {
        for (int index = 0; index < columns; index++) {
            sheet.setColumnWidth(index, 22 * 256);
        }
    }

    private record ReportScope(UUID doctorId, UUID hospitalId) {
    }

    private record ExportFilters(UUID procedureId, UUID patientId, UUID doctorId, UUID hospitalId) {
    }
}
