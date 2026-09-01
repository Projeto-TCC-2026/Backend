package com.tcc.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tcc.domain.model.Doctor;
import com.tcc.domain.model.Hospital;
import com.tcc.domain.model.Checkin;
import com.tcc.domain.model.PatientProcedure;
import com.tcc.domain.model.Procedure;
import com.tcc.domain.model.Role;
import com.tcc.domain.model.User;
import com.tcc.domain.repository.AlertRepository;
import com.tcc.domain.repository.CheckinRepository;
import com.tcc.domain.repository.DoctorProcedureRepository;
import com.tcc.domain.repository.DoctorRepository;
import com.tcc.domain.repository.PatientProcedureRepository;
import com.tcc.domain.repository.PatientRepository;
import com.tcc.domain.repository.ProcedureRepository;
import com.tcc.domain.repository.UserRepository;
import com.tcc.exception.UnauthorizedException;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReportServiceImplTest {

    @Mock private PatientRepository patientRepository;
    @Mock private DoctorRepository doctorRepository;
    @Mock private ProcedureRepository procedureRepository;
    @Mock private UserRepository userRepository;
    @Mock private DoctorProcedureRepository doctorProcedureRepository;
    @Mock private PatientProcedureRepository patientProcedureRepository;
    @Mock private CheckinRepository checkinRepository;
    @Mock private AlertRepository alertRepository;

    @Test
    void hospitalExportIsScopedAndProducesValidEmptyWorkbook() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setActive(true);
        User user = new User("hospital@tcc.com", "hash", Role.HOSPITAL);
        user.setHospital(hospital);
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setHospital(hospital);
        LocalDate date = LocalDate.of(2026, 8, 31);

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        when(procedureRepository.findById(procedure.getId())).thenReturn(Optional.of(procedure));
        when(checkinRepository.findForReport(any(), any(), eq(procedure.getId()), eq(null), eq(null),
                eq(hospital.getId()))).thenReturn(List.of());

        byte[] export = service().exportCheckins(user.getEmail(), date, date, procedure.getId(), null, null);

        assertThat(export).isNotEmpty();
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export))) {
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("Data/hora de envio");
        }
        verify(checkinRepository).findForReport(date.atStartOfDay(), date.plusDays(1).atStartOfDay(),
                procedure.getId(), null, null, hospital.getId());
    }

    @Test
    void doctorCannotNarrowReportToAnotherDoctor() {
        Doctor ownDoctor = new Doctor();
        ownDoctor.setId(UUID.randomUUID());
        ownDoctor.setActive(true);
        User user = new User("doctor@tcc.com", "hash", Role.DOCTOR);
        user.setId(UUID.randomUUID());

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        when(doctorRepository.findByUserId(user.getId())).thenReturn(Optional.of(ownDoctor));

        assertThatThrownBy(() -> service().exportAlerts(user.getEmail(), null, null, null, null, UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("não corresponde");
    }

    @Test
    void hospitalCanExportPatientReportWithoutProcedureAssignment() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setActive(true);
        User user = new User("hospital@tcc.com", "hash", Role.HOSPITAL);
        user.setHospital(hospital);
        UUID patientId = UUID.randomUUID();

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setHospital(hospital);
        LocalDate date = LocalDate.of(2026, 8, 31);

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        when(procedureRepository.findById(procedure.getId())).thenReturn(Optional.of(procedure));
        when(checkinRepository.findForReport(any(), any(), eq(procedure.getId()), eq(patientId), eq(null),
                eq(hospital.getId()))).thenReturn(List.of());

        assertThat(service().exportCheckins(user.getEmail(), date, date, procedure.getId(), patientId, null))
                .isNotEmpty();
    }

    @Test
    void unfilteredCheckinExportCreatesOneSheetPerProcedure() throws Exception {
        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setActive(true);
        User user = new User("hospital@tcc.com", "hash", Role.HOSPITAL);
        user.setHospital(hospital);
        Procedure firstProcedure = procedure(hospital, "Artroscopia");
        Procedure secondProcedure = procedure(hospital, "Meniscectomia");
        Checkin firstCheckin = checkinFor(firstProcedure);
        Checkin secondCheckin = checkinFor(secondProcedure);
        LocalDate date = LocalDate.of(2026, 8, 31);

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        when(checkinRepository.findForReport(any(), any(), eq(null), eq(null), eq(null), eq(hospital.getId())))
                .thenReturn(List.of(firstCheckin, secondCheckin));

        byte[] export = service().exportCheckins(user.getEmail(), date, date, null, null, null);

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(export))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheet("Artroscopia")).isNotNull();
            assertThat(workbook.getSheet("Meniscectomia")).isNotNull();
        }
    }

    @Test
    void alertExportUsesTheSelectedDateAsQueryBounds() {
        Hospital hospital = new Hospital();
        hospital.setId(UUID.randomUUID());
        hospital.setActive(true);
        User user = new User("hospital@tcc.com", "hash", Role.HOSPITAL);
        user.setHospital(hospital);
        LocalDate date = LocalDate.of(2026, 8, 5);

        when(userRepository.findByEmailWithHospital(user.getEmail())).thenReturn(Optional.of(user));
        when(alertRepository.findForReport(date.atStartOfDay(), date.plusDays(1).atStartOfDay()))
                .thenReturn(List.of());

        assertThat(service().exportAlerts(user.getEmail(), date, date, null, null, null)).isNotEmpty();

        verify(alertRepository).findForReport(date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }

    private Procedure procedure(Hospital hospital, String title) {
        Procedure procedure = new Procedure();
        procedure.setId(UUID.randomUUID());
        procedure.setHospital(hospital);
        procedure.setTitle(title);
        return procedure;
    }

    private Checkin checkinFor(Procedure procedure) {
        PatientProcedure assignment = new PatientProcedure();
        assignment.setProcedure(procedure);
        Checkin checkin = new Checkin();
        checkin.setPatientProcedure(assignment);
        return checkin;
    }

    private ReportServiceImpl service() {
        return new ReportServiceImpl(patientRepository, doctorRepository, procedureRepository, userRepository,
                doctorProcedureRepository, patientProcedureRepository, checkinRepository, alertRepository);
    }
}
