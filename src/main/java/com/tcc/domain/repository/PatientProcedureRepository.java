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

    List<PatientProcedure> findByPatientId(UUID patientId);

    List<PatientProcedure> findByDoctorId(UUID doctorId);

    List<PatientProcedure> findByProcedureId(UUID procedureId);

    Page<PatientProcedure> findByPatientIdAndDoctorId(UUID patientId, UUID doctorId, Pageable pageable);

    Optional<PatientProcedure> findByIdAndPatientIdAndDoctorId(UUID id, UUID patientId, UUID doctorId);

    boolean existsByPatientIdAndProcedureIdAndDoctorId(UUID patientId, UUID procedureId, UUID doctorId);

    boolean existsByPatientIdAndDoctorId(UUID patientId, UUID doctorId);

    @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PatientProcedure pp "
            + "WHERE pp.patient.id = :patientId AND pp.doctor.hospital.id = :hospitalId "
            + "AND (:procedureId IS NULL OR pp.procedure.id = :procedureId)")
    boolean existsInHospitalScope(@Param("patientId") UUID patientId,
                                  @Param("hospitalId") UUID hospitalId,
                                  @Param("procedureId") UUID procedureId);
}
