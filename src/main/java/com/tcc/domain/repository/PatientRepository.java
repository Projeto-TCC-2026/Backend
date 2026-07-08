package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    
    Optional<Patient> findByUserId(Long userId);
    
    Optional<Patient> findByCpf(String cpf);
    
    boolean existsByCpf(String cpf);
    
    // Buscar apenas pacientes ativos (sem paginação)
    List<Patient> findByActiveTrue();
    
    // Buscar apenas pacientes ativos (com paginação)
    Page<Patient> findByActiveTrue(Pageable pageable);
    
    // Buscar paciente ativo por ID
    Optional<Patient> findByIdAndActiveTrue(Long id);
    
    // Verificar se CPF já existe em paciente ativo
    boolean existsByCpfAndActiveTrue(String cpf);
    
    // Buscar por nome (parcial, case-insensitive) - apenas ativos
    Page<Patient> findByFullNameContainingIgnoreCaseAndActiveTrue(String name, Pageable pageable);
    
    // Buscar por CPF - apenas ativos
    Page<Patient> findByCpfContainingAndActiveTrue(String cpf, Pageable pageable);
    
    // Buscar por email - apenas ativos
    Page<Patient> findByEmailContainingIgnoreCaseAndActiveTrue(String email, Pageable pageable);
    
    Optional<Patient> findByEmailAndActiveTrue(String email);
    
    boolean existsByEmailAndActiveTrue(String email);
    
    // Buscar por telefone - apenas ativos
    Page<Patient> findByPhoneContainingAndActiveTrue(String phone, Pageable pageable);
    
    // Filtro avançado com múltiplos critérios
    @Query("SELECT p FROM Patient p WHERE p.active = true " +
           "AND (:name IS NULL OR LOWER(p.fullName) LIKE LOWER(CONCAT('%', :name, '%'))) " +
           "AND (:gender IS NULL OR p.gender = :gender) " +
           "AND (:city IS NULL OR LOWER(p.city) LIKE LOWER(CONCAT('%', :city, '%'))) " +
           "AND (:state IS NULL OR p.state = :state)")
    Page<Patient> findByFilters(@Param("name") String name,
                                 @Param("gender") String gender,
                                 @Param("city") String city,
                                 @Param("state") String state,
                                 Pageable pageable);
}


