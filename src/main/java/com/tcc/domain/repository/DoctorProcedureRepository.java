package com.tcc.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.tcc.domain.model.DoctorProcedure;

@Repository
public interface DoctorProcedureRepository extends JpaRepository<DoctorProcedure, UUID> {

    List<DoctorProcedure> findByProcedureIdAndActiveTrue(UUID procedureId);

    List<DoctorProcedure> findByDoctorIdAndActiveTrue(UUID doctorId);

    Optional<DoctorProcedure> findByDoctorIdAndProcedureIdAndActiveTrue(UUID doctorId, UUID procedureId);

    Optional<DoctorProcedure> findByIdAndActiveTrue(UUID id);

    boolean existsByDoctorIdAndProcedureIdAndActiveTrue(UUID doctorId, UUID procedureId);

    /**
     * Busca o vínculo ignorando o active. Necessário porque uq_doctor_procedures_pair
     * impede inserir um segundo par (doctor_id, procedure_id): reatrelar um médico
     * reaproveita a linha inativada em vez de criar outra.
     */
    Optional<DoctorProcedure> findByDoctorIdAndProcedureId(UUID doctorId, UUID procedureId);

    long countByProcedureIdAndActiveTrue(UUID procedureId);

    long countByDoctorIdAndActiveTrue(UUID doctorId);

    @Query("SELECT dp FROM DoctorProcedure dp JOIN FETCH dp.procedure WHERE dp.doctor.user.id = :userId AND dp.active = true AND dp.procedure.active = true ORDER BY dp.createdAt")
    List<DoctorProcedure> findActiveByDoctorUserId(@Param("userId") UUID userId);
}
