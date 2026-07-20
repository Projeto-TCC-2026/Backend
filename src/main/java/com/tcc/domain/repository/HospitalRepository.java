package com.tcc.domain.repository;

import com.tcc.domain.model.Hospital;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface HospitalRepository extends JpaRepository<Hospital, Long> {
    
    Optional<Hospital> findByCnpj(String cnpj);
    
    boolean existsByCnpj(String cnpj);
    
    // Busca por nome (case-insensitive, busca parcial)
    Page<Hospital> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    // Filtros combinados
    @Query("SELECT h FROM Hospital h WHERE " +
           "(:name IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:city IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:state IS NULL OR UPPER(h.state) = UPPER(:state))")
    Page<Hospital> findByFilters(
        @Param("name") String name,
        @Param("city") String city,
        @Param("state") String state,
        Pageable pageable
    );
    
    // Dashboard queries - Consultas otimizadas com COUNT
    @Query("SELECT COUNT(h) FROM Hospital h")
    Long countTotalHospitals();
    
    // Note: Hospital doesn't have active field, so we count all as active
    // If you want to add active/inactive logic, add the field to Hospital entity first
}
