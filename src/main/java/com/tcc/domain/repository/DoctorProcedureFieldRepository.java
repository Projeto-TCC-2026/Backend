package com.tcc.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcc.domain.model.DoctorProcedureField;

public interface DoctorProcedureFieldRepository extends JpaRepository<DoctorProcedureField, UUID> {
  List<DoctorProcedureField> findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(UUID doctorProcedureId);

  @Query("""
      SELECT f
      FROM DoctorProcedureField f
      JOIN f.doctorProcedure dp
      WHERE dp.procedure.id = :procedureId
        AND (:doctorId IS NULL OR dp.doctor.id = :doctorId)
        AND (:hospitalId IS NULL OR dp.doctor.hospital.id = :hospitalId)
      ORDER BY dp.doctor.id, f.displayOrder, f.name
      """)
  List<DoctorProcedureField> findMetricsFields(@Param("procedureId") UUID procedureId,
      @Param("doctorId") UUID doctorId,
      @Param("hospitalId") UUID hospitalId);
}
