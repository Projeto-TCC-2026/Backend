package com.tcc.domain.repository;

import com.tcc.domain.model.Patient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}
