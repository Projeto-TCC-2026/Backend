package com.tcc.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tcc.domain.model.PatientProcedure;

@Repository
public interface PatientProcedureRepository extends JpaRepository<PatientProcedure, UUID> {

    List<PatientProcedure> findByPatientIdAndActiveTrue(UUID patientId);

    List<PatientProcedure> findByDoctorIdAndActiveTrue(UUID doctorId);

    List<PatientProcedure> findByProcedureIdAndActiveTrue(UUID procedureId);

    Page<PatientProcedure> findByPatientIdAndDoctorIdAndActiveTrue(UUID patientId, UUID doctorId, Pageable pageable);

    Optional<PatientProcedure> findByIdAndPatientIdAndDoctorIdAndActiveTrue(UUID id, UUID patientId, UUID doctorId);

    Optional<PatientProcedure> findByIdAndActiveTrue(UUID id);

    boolean existsByPatientIdAndProcedureIdAndDoctorIdAndActiveTrue(UUID patientId, UUID procedureId, UUID doctorId);

    boolean existsByPatientIdAndDoctorIdAndActiveTrue(UUID patientId, UUID doctorId);

    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PatientProcedure pp "
            + "WHERE pp.patient.id = :patientId AND pp.doctor.hospital.id = :hospitalId "
            + "AND pp.active = true "
            + "AND (:procedureId IS NULL OR pp.procedure.id = :procedureId)")
    boolean existsInHospitalScope(@Param("patientId") UUID patientId,
                                  @Param("hospitalId") UUID hospitalId,
                                  @Param("procedureId") UUID procedureId);
}
