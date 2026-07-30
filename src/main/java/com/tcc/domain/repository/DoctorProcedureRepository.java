package com.tcc.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.tcc.domain.model.DoctorProcedure;

@Repository
public interface DoctorProcedureRepository extends JpaRepository<DoctorProcedure, UUID> {

    List<DoctorProcedure> findByProcedureId(UUID procedureId);

    List<DoctorProcedure> findByDoctorId(UUID doctorId);

    Optional<DoctorProcedure> findByDoctorIdAndProcedureId(UUID doctorId, UUID procedureId);

    boolean existsByDoctorIdAndProcedureId(UUID doctorId, UUID procedureId);

    long countByProcedureId(UUID procedureId);

    long countByDoctorId(UUID doctorId);
}
