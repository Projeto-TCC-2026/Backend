package com.tcc.domain.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.tcc.application.dto.response.HospitalSummary;
import com.tcc.domain.model.Hospital;

public interface HospitalRepository extends JpaRepository<Hospital, UUID> {
    
    Optional<Hospital> findByCnpj(String cnpj);
    
    boolean existsByCnpj(String cnpj);
    
    long countByActiveTrue();
    
    long countByActiveFalse();
    
    Page<Hospital> findByNameContainingIgnoreCase(String name, Pageable pageable);
    
    Page<Hospital> findByActive(Boolean active, Pageable pageable);
    
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
    
    @Query("SELECT h FROM Hospital h WHERE " +
           "(:name IS NULL OR LOWER(h.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:city IS NULL OR LOWER(h.city) LIKE LOWER(CONCAT('%', :city, '%'))) AND " +
           "(:state IS NULL OR UPPER(h.state) = UPPER(:state)) AND " +
           "h.active = :active")
    Page<Hospital> findByFiltersAndActive(
        @Param("name") String name,
        @Param("city") String city,
        @Param("state") String state,
        @Param("active") Boolean active,
        Pageable pageable
    );
    
    @Query("SELECT new com.tcc.application.dto.response.HospitalSummary(" +
           "h.id, h.name, h.cnpj, h.city, h.state, h.phone, h.email, " +
           "CAST(COUNT(d.id) AS long), h.active) " +
           "FROM Hospital h LEFT JOIN h.doctors d " +
           "GROUP BY h.id, h.name, h.cnpj, h.city, h.state, h.phone, h.email, h.active")
    Page<HospitalSummary> findHospitalsSummary(Pageable pageable);
    
    @Query("SELECT COUNT(h) FROM Hospital h")
    Long countTotalHospitals();
}
