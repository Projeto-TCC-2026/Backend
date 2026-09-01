package com.tcc.presentation.controller;

import com.tcc.application.dto.response.ApiResponse;
import com.tcc.application.dto.response.DoctorsByHospitalResponse;
import com.tcc.application.dto.response.PatientsByHospitalResponse;
import com.tcc.application.dto.response.ProceduresByDoctorResponse;
import com.tcc.application.dto.response.ProceduresByPeriodResponse;
import com.tcc.application.service.ReportService;
import com.tcc.exception.BusinessException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reports")
@Tag(name = "Relatórios", description = "Endpoints para geração de relatórios e análises de dados")
@SecurityRequirement(name = "Bearer Authentication")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/patients-by-hospital")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Pacientes por Hospital",
        description = "Retorna a quantidade de pacientes ativos agrupados por hospital. " +
                      "Os resultados são ordenados por quantidade de pacientes (decrescente). " +
                      "Acesso restrito a administradores."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relatório retornado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<List<PatientsByHospitalResponse>>> getPatientsByHospital() {
        List<PatientsByHospitalResponse> report = reportService.getPatientsByHospital();
        ApiResponse<List<PatientsByHospitalResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctors-by-hospital")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Doutores por Hospital",
        description = "Retorna a quantidade de doutores agrupados por hospital. " +
                      "Os resultados são ordenados por quantidade de doutores (decrescente). " +
                      "Acesso restrito a administradores."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relatório retornado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<List<DoctorsByHospitalResponse>>> getDoctorsByHospital() {
        List<DoctorsByHospitalResponse> report = reportService.getDoctorsByHospital();
        ApiResponse<List<DoctorsByHospitalResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/procedures-by-doctor")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Procedimentos por Doutor",
        description = "Retorna a quantidade de procedimentos ativos agrupados por doutor. " +
                      "Inclui informações do doutor (nome, especialidade) e total de procedimentos. " +
                      "Os resultados são ordenados por quantidade de procedimentos (decrescente)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relatório retornado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<List<ProceduresByDoctorResponse>>> getProceduresByDoctor() {
        List<ProceduresByDoctorResponse> report = reportService.getProceduresByDoctor();
        ApiResponse<List<ProceduresByDoctorResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/procedures-by-period")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Relatório de Procedimentos por Período",
        description = "Retorna a quantidade de procedimentos ativos agrupados por período (mês/ano). " +
                      "Requer data de início e fim para o período de análise. " +
                      "Os resultados são ordenados por período (decrescente)."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Relatório retornado com sucesso"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Período inválido"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Não autenticado"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Acesso negado")
    })
    public ResponseEntity<ApiResponse<List<ProceduresByPeriodResponse>>> getProceduresByPeriod(
            @Parameter(
                description = "Data de início do período (formato: yyyy-MM-dd'T'HH:mm:ss)",
                example = "2026-01-01T00:00:00",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(
                description = "Data de fim do período (formato: yyyy-MM-dd'T'HH:mm:ss)",
                example = "2026-12-31T23:59:59",
                required = true
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        List<ProceduresByPeriodResponse> report = reportService.getProceduresByPeriod(startDate, endDate);
        ApiResponse<List<ProceduresByPeriodResponse>> response = ApiResponse.success(report);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/checkins/daily/export")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'DOCTOR')")
    public ResponseEntity<byte[]> exportDailyCheckins(
            Authentication authentication,
            @RequestParam String date,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId) {
        LocalDate reportDate = parseDate(date, "data");
        return xlsx(reportService.exportCheckins(email(authentication), reportDate, reportDate,
                procedureId, patientId, doctorId), "checkins-daily-" + reportDate + ".xlsx");
    }

    @GetMapping("/checkins/weekly/export")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'DOCTOR')")
    public ResponseEntity<byte[]> exportWeeklyCheckins(
            Authentication authentication,
            @RequestParam String week,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId) {
        LocalDate startDate = parseIsoWeek(week);
        return xlsx(reportService.exportCheckins(email(authentication), startDate, startDate.plusDays(6),
                procedureId, patientId, doctorId), "checkins-weekly-" + week + ".xlsx");
    }

    @GetMapping("/checkins/monthly/export")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'DOCTOR')")
    public ResponseEntity<byte[]> exportMonthlyCheckins(
            Authentication authentication,
            @RequestParam String month,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId) {
        final YearMonth reportMonth;
        try {
            reportMonth = YearMonth.parse(month);
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Mês inválido. Use o formato YYYY-MM");
        }
        return xlsx(reportService.exportCheckins(email(authentication), reportMonth.atDay(1),
                reportMonth.atEndOfMonth(), procedureId, patientId, doctorId),
                "checkins-monthly-" + month + ".xlsx");
    }

    @GetMapping("/alerts/export")
    @PreAuthorize("hasAnyRole('HOSPITAL', 'DOCTOR')")
    public ResponseEntity<byte[]> exportAlerts(
            Authentication authentication,
            @RequestParam String date,
            @RequestParam(required = false) UUID procedureId,
            @RequestParam(required = false) UUID patientId,
            @RequestParam(required = false) UUID doctorId) {
        LocalDate reportDate = parseDate(date, "data");
        return xlsx(reportService.exportAlerts(email(authentication), reportDate, reportDate,
                procedureId, patientId, doctorId), "alerts-" + reportDate + ".xlsx");
    }

    private ResponseEntity<byte[]> xlsx(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }

    private LocalDate parseDate(String value, String label) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new BusinessException(label + " inválida. Use o formato yyyy-MM-dd");
        }
    }

    private LocalDate parseIsoWeek(String week) {
        if (!week.matches("\\d{4}-W\\d{2}")) {
            throw new BusinessException("Semana inválida. Use o formato YYYY-Www");
        }
        int year = Integer.parseInt(week.substring(0, 4));
        int weekNumber = Integer.parseInt(week.substring(6));
        if (weekNumber < 1 || weekNumber > 53) {
            throw new BusinessException("Semana inválida. Use uma semana ISO válida");
        }
        LocalDate firstMonday = LocalDate.of(year, 1, 4)
                .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startDate = firstMonday.plusWeeks(weekNumber - 1L);
        if (startDate.get(WeekFields.ISO.weekBasedYear()) != year
                || startDate.get(WeekFields.ISO.weekOfWeekBasedYear()) != weekNumber) {
            throw new BusinessException("Semana inválida. Use uma semana ISO válida");
        }
        return startDate;
    }

    private String email(Authentication authentication) {
        return ((UserDetails) authentication.getPrincipal()).getUsername();
    }
}
