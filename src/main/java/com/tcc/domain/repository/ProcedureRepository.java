package com.tcc.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tcc.domain.model.Procedure;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {

    Page<Procedure> findByHospitalId(UUID hospitalId, Pageable pageable);

    Page<Procedure> findByHospitalIdAndActive(UUID hospitalId, Boolean active, Pageable pageable);

    Optional<Procedure> findByIdAndHospitalId(UUID id, UUID hospitalId);

    boolean existsByHospitalIdAndTitleIgnoreCase(UUID hospitalId, String title);

    List<Procedure> findByActive(Boolean active);

    @Query("SELECT p FROM Procedure p " +
           "JOIN p.doctorProcedures dp " +
           "WHERE dp.doctor.id = :doctorId " +
           "AND p.active = true")
    List<Procedure> findActiveByDoctorId(@Param("doctorId") UUID doctorId);

    @Query("SELECT COUNT(p) FROM Procedure p WHERE p.active = true")
    Long countActiveProceduresTotal();

    @Query("SELECT COUNT(p) FROM Procedure p " +
           "WHERE p.hospital.id = :hospitalId " +
           "AND p.active = true")
    Long countActiveProceduresByHospitalId(@Param("hospitalId") UUID hospitalId);

    // Conta procedimentos atribuídos a cada médico, via doctor_procedures
    @Query("SELECT dp.doctor.id as doctorId, dp.doctor.fullName as doctorName, " +
           "dp.doctor.specialty as specialty, COUNT(dp) as totalProcedures " +
           "FROM DoctorProcedure dp " +
           "WHERE dp.procedure.active = true " +
           "GROUP BY dp.doctor.id, dp.doctor.fullName, dp.doctor.specialty " +
           "ORDER BY totalProcedures DESC")
    List<Object[]> countProceduresByDoctor();

    @Query("SELECT YEAR(p.createdAt) as year, MONTH(p.createdAt) as month, COUNT(p) as totalProcedures " +
           "FROM Procedure p " +
           "WHERE p.active = true " +
           "AND p.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> countProceduresByPeriod(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT YEAR(p.createdAt) as year, MONTH(p.createdAt) as month, COUNT(p) as totalProcedures " +
           "FROM Procedure p " +
           "WHERE p.active = true " +
           "AND p.hospital.id = :hospitalId " +
           "AND p.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> countProceduresByPeriodAndHospitalId(
        @Param("hospitalId") UUID hospitalId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
