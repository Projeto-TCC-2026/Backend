package com.tcc.domain.repository;

import com.tcc.domain.model.Procedure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProcedureRepository extends JpaRepository<Procedure, UUID> {
    
    List<Procedure> findByDoctorId(UUID doctorId);
    
    List<Procedure> findByDoctorIdAndActive(UUID doctorId, Boolean active);
    
    List<Procedure> findByActive(Boolean active);
    
    @Query("SELECT COUNT(p) FROM Procedure p WHERE p.active = true")
    Long countActiveProceduresTotal();
    
    @Query("SELECT COUNT(p) FROM Procedure p " +
           "WHERE p.doctor.hospital.id = :hospitalId " +
           "AND p.active = true")
    Long countActiveProceduresByHospitalId(@Param("hospitalId") UUID hospitalId);
    
    @Query("SELECT p.doctor.id as doctorId, p.doctor.fullName as doctorName, " +
           "p.doctor.specialty as specialty, COUNT(p) as totalProcedures " +
           "FROM Procedure p " +
           "WHERE p.active = true " +
           "GROUP BY p.doctor.id, p.doctor.fullName, p.doctor.specialty " +
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
           "AND p.doctor.hospital.id = :hospitalId " +
           "AND p.createdAt BETWEEN :startDate AND :endDate " +
           "GROUP BY YEAR(p.createdAt), MONTH(p.createdAt) " +
           "ORDER BY year DESC, month DESC")
    List<Object[]> countProceduresByPeriodAndHospitalId(
        @Param("hospitalId") UUID hospitalId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}
