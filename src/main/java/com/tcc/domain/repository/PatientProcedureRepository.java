package com.tcc.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tcc.domain.model.PatientProcedure;

@Repository
public interface PatientProcedureRepository extends JpaRepository<PatientProcedure, UUID> {

    List<PatientProcedure> findByPatientId(UUID patientId);

    List<PatientProcedure> findByDoctorId(UUID doctorId);

    List<PatientProcedure> findByProcedureId(UUID procedureId);

    Page<PatientProcedure> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId, Pageable pageable);

    Optional<PatientProcedure> findByIdAndPatientIdAndDoctorId(UUID id, UUID patientId, UUID doctorId);

    boolean existsByPatientIdAndProcedureIdAndDoctorId(UUID patientId, UUID procedureId, UUID doctorId);
}
