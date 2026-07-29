package com.tcc.domain.repository;

import com.tcc.domain.model.Doctor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
    
    Optional<Doctor> findByUserId(UUID userId);
    
    Optional<Doctor> findByCpf(String cpf);
    
    Optional<Doctor> findByCrm(String crm);
    
    List<Doctor> findByHospitalId(UUID hospitalId);

    Page<Doctor> findByHospitalId(UUID hospitalId, Pageable pageable);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByCrm(String crm);
    
    Page<Doctor> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    
    Page<Doctor> findBySpecialtyContainingIgnoreCase(String specialty, Pageable pageable);
    
    @Query("SELECT d FROM Doctor d WHERE " +
           "(:hospitalId IS NULL OR d.hospital.id = :hospitalId) AND " +
           "(:specialty IS NULL OR LOWER(d.specialty) LIKE LOWER(CONCAT('%', :specialty, '%'))) AND " +
           "(:name IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:crm IS NULL OR LOWER(d.crm) LIKE LOWER(CONCAT('%', :crm, '%')))")
    Page<Doctor> findByFilters(
        @Param("hospitalId") UUID hospitalId,
        @Param("specialty") String specialty,
        @Param("name") String name,
        @Param("crm") String crm,
        Pageable pageable
    );
    
    @Query("SELECT COUNT(d) FROM Doctor d")
    Long countTotalDoctors();
    
    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.hospital.id = :hospitalId")
    Long countByHospitalId(@Param("hospitalId") UUID hospitalId);
    
    @Query("SELECT d.hospital.id as hospitalId, d.hospital.name as hospitalName, COUNT(d) as totalDoctors " +
           "FROM Doctor d " +
           "GROUP BY d.hospital.id, d.hospital.name " +
           "ORDER BY totalDoctors DESC")
    List<Object[]> countDoctorsByHospital();
}
