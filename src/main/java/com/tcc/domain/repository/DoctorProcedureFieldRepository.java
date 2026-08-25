package com.tcc.domain.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.tcc.domain.model.DoctorProcedureField;

public interface DoctorProcedureFieldRepository extends JpaRepository<DoctorProcedureField, UUID> {
  List<DoctorProcedureField> findByDoctorProcedureIdAndActiveTrueOrderByDisplayOrderAsc(UUID doctorProcedureId);
}
