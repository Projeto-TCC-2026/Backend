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

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    
    Optional<Doctor> findByUserId(Long userId);
    
    Optional<Doctor> findByCpf(String cpf);
    
    Optional<Doctor> findByCrm(String crm);
    
    List<Doctor> findByHospitalId(Long hospitalId);
    
    boolean existsByCpf(String cpf);
    
    boolean existsByCrm(String crm);
    
    // Busca por nome (case-insensitive, busca parcial)
    Page<Doctor> findByFullNameContainingIgnoreCase(String fullName, Pageable pageable);
    
    // Busca por especialidade (case-insensitive, busca parcial)
    Page<Doctor> findBySpecialtyContainingIgnoreCase(String specialty, Pageable pageable);
    
    // Filtros combinados
    @Query("SELECT d FROM Doctor d WHERE " +
           "(:hospitalId IS NULL OR d.hospital.id = :hospitalId) AND " +
           "(:specialty IS NULL OR LOWER(d.specialty) LIKE LOWER(CONCAT('%', :specialty, '%'))) AND " +
           "(:name IS NULL OR LOWER(d.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:crm IS NULL OR LOWER(d.crm) LIKE LOWER(CONCAT('%', :crm, '%')))")
    Page<Doctor> findByFilters(
        @Param("hospitalId") Long hospitalId,
        @Param("specialty") String specialty,
        @Param("name") String name,
        @Param("crm") String crm,
        Pageable pageable
    );
}
